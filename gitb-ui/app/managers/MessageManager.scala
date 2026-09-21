/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package managers

import actors.BulkTaskActor
import actors.events.messaging.CreateMessageUnreadStatus
import managers.MessageManager.{MessageSenderVisibility, ViewerUserContext}
import models.Enums.{MessagePeerType, MessageTargetType}
import models._
import org.apache.pekko.actor.ActorSystem
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{HtmlUtil, TimeUtil}

import javax.inject.{Inject, Singleton}
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

object MessageManager {

  case class MessageSenderVisibility(allowOrganisationSenderNames: Boolean, allowAdminSenderNames: Boolean)
  case class ViewerUserContext(orgId: Long, isTestBedAdmin: Boolean)
}

/**
 * Manages the internal (organisation-to-organisation) messaging feature: messages and their per-
 * recipient delivery/read state ("My messages" screen). See MessageService for the REST endpoints and
 * AuthorizationManager for the send-time recipient authorisation.
 */
@Singleton
class MessageManager @Inject() (dbConfigProvider: DatabaseConfigProvider, actorSystem: ActorSystem)
                                (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  /** Cap applied to the body preview shown in the message listing, to keep list payloads bounded. */
  private val PreviewMaxLength = 300
  private val Logger = LoggerFactory.getLogger(classOf[MessageManager])

  private def previewText(bodyText: Option[String]): Option[String] = {
    bodyText.map { text =>
      if (text.length > PreviewMaxLength) text.take(PreviewMaxLength) + "…" else text
    }
  }

  /**
   * The kind of organisation `org` is, from a messaging point of view - captured once at send time.
   */
  private def peerType(isAdminOrganisation: Boolean, orgCommunityId: Long): Short = {
    if (!isAdminOrganisation) MessagePeerType.Organisation.id.toShort
    else if (orgCommunityId == Constants.DefaultCommunityId) MessagePeerType.TestBedAdmin.id.toShort
    else MessagePeerType.CommunityAdmin.id.toShort
  }

  /**
   * The frozen snapshot text recorded for an organisation at send time, by its peer type.
   */
  private def peerNameSnapshot(peerType: Short, orgFullName: String, communityFullName: String): String = {
    val name = MessagePeerType(peerType) match {
      case MessagePeerType.Organisation => orgFullName
      case MessagePeerType.CommunityAdmin => communityFullName
      case MessagePeerType.TestBedAdmin => "Test Bed administrator"
    }
    if (name.length > 254) name.take(254) else name
  }

  /**
   * Viewer-facing organisation/community display name for a sender or recipient.
   */
  private def resolvePeerDisplayName(peerType: Short, nameSnapshot: String, viewerIsTestBedAdmin: Boolean): String = {
    MessagePeerType(peerType) match {
      case MessagePeerType.CommunityAdmin =>
        if (viewerIsTestBedAdmin) s"Community administrator ($nameSnapshot)" else "Community administrator"
      case MessagePeerType.TestBedAdmin => "Test Bed administrator"
      case MessagePeerType.Organisation => nameSnapshot
    }
  }

  /**
   * Whether a message sender user's name should be revealed to the requesting viewer.
   */
  private def resolveSenderUserName(senderType: Short, senderUserNameSnapshot: Option[String],
                                     viewerIsTestBedAdmin: Boolean, viewerIsCommunityAdmin: Boolean,
                                    senderVisibility: MessageSenderVisibility): Option[String] = {
    senderUserNameSnapshot.filter(_.nonEmpty).flatMap { name =>
      if (viewerIsTestBedAdmin) {
        // See all users' names
        Some(name)
      } else if (viewerIsCommunityAdmin) {
        // See all users' names except the Test Bed admin's
        if (senderType == MessagePeerType.TestBedAdmin.id.toShort) None else Some(name)
      } else {
        // Depends on community permissions, however Test Bed admin is always hidden.
        MessagePeerType(senderType) match {
          case MessagePeerType.Organisation => if (senderVisibility.allowOrganisationSenderNames) Some(name) else None
          case MessagePeerType.CommunityAdmin => if (senderVisibility.allowAdminSenderNames) Some(name) else None
          case MessagePeerType.TestBedAdmin => None
        }
      }
    }
  }

  /**
   * The viewer's own community's two sender-name permission flags, as (allowOrganisationSenderNames,
   * allowAdminSenderNames).
   */
  private def loadSenderNamePermissions(orgId: Long, viewerIsTestBedAdmin: Boolean, viewerIsCommunityAdmin: Boolean, needed: Boolean): DBIO[MessageSenderVisibility] = {
    if (!needed || viewerIsTestBedAdmin || viewerIsCommunityAdmin) {
      DBIO.successful(MessageSenderVisibility(allowOrganisationSenderNames = false, allowAdminSenderNames = false))
    } else {
      PersistenceSchema.organizations
        .join(PersistenceSchema.communities).on(_.community === _.id)
        .filter(_._1.id === orgId)
        .map(x => (x._2.allowOrganisationSenderNames, x._2.allowAdminSenderNames))
        .result
        .head
        .map(x => MessageSenderVisibility(x._1, x._2))
    }
  }

  /**
   * Resolves a recipient descriptor to a set of organisation ids.
   */
  private def resolveTargets(senderOrgId: Long, senderCommunityId: Long, targets: List[MessageTarget]): DBIO[Set[Long]] = {
    val actions = targets.map { target =>
      MessageTargetType.apply(target.targetType) match {
        case MessageTargetType.OwnOrganisation =>
          DBIO.successful(Set(senderOrgId))
        case MessageTargetType.CommunityAdmin =>
          val communityId = target.communityId.getOrElse(senderCommunityId)
          PersistenceSchema.organizations.filter(_.community === communityId).filter(_.adminOrganization === true).map(_.id).result.map(_.toSet)
        case MessageTargetType.TestBedAdmin =>
          PersistenceSchema.organizations.filter(_.community === Constants.DefaultCommunityId).filter(_.adminOrganization === true).map(_.id).result.map(_.toSet)
        case MessageTargetType.AllCommunityMembers =>
          val communityId = target.communityId.getOrElse(senderCommunityId)
          PersistenceSchema.organizations.filter(_.community === communityId).filter(_.adminOrganization === false).map(_.id).result.map(_.toSet)
        case MessageTargetType.Organisation =>
          DBIO.successful(target.organisationId.toSet)
        case MessageTargetType.AllCommunityAdmins =>
          PersistenceSchema.organizations.filter(_.adminOrganization === true).filter(_.id =!= Constants.DefaultCommunityId).map(_.id).result.map(_.toSet)
        case MessageTargetType.AllOrganisations =>
          PersistenceSchema.organizations.filter(_.adminOrganization === false).map(_.id).result.map(_.toSet)
        case MessageTargetType.AllUsers =>
          PersistenceSchema.organizations.map(_.id).result.map(_.toSet)
        case MessageTargetType.AllCommunityUsers =>
          PersistenceSchema.organizations.filter(_.community === target.communityId).map(_.id).result.map(_.toSet)
      }
    }
    DBIO.sequence(actions).map(_.flatten.toSet)
  }

  private def prepareBody(rawBody: Option[String]): (Option[String], Option[String]) = {
    val sanitizedBody = rawBody.map(HtmlUtil.sanitizeMinimalEditorContent).filter(_.nonEmpty)
    val bodyText = sanitizedBody.map(HtmlUtil.toPlainText).filter(_.nonEmpty)
    (sanitizedBody, bodyText)
  }

  def createMessage(senderUserId: Long, subject: Option[String], rawBody: Option[String], important: Boolean, targets: List[MessageTarget]): Future[Long] = {
    val (sanitizedBody, bodyText) = prepareBody(rawBody)
    DB.run(createMessageInternal(senderUserId, subject.filter(_.nonEmpty), sanitizedBody, bodyText, important, Left(targets), None).transactionally)
      .map { messageId => dispatchUnreadStatusCreation(messageId, senderUserId); messageId }
  }

  def createMessageReply(senderUserId: Long, parentMessageId: Long, subject: Option[String], rawBody: Option[String], important: Boolean, targets: List[MessageTarget]): Future[Long] = {
    val (sanitizedBody, bodyText) = prepareBody(rawBody)
    DB.run(
      (for {
        messageId <- createMessageInternal(senderUserId, subject.filter(_.nonEmpty), sanitizedBody, bodyText, important, Left(targets), Some(parentMessageId))
      } yield messageId).transactionally
    ).map { messageId => dispatchUnreadStatusCreation(messageId, senderUserId); messageId }
  }

  /**
   * Fire-and-forget dispatch to BulkTaskActor, called only once the message and its recipient rows have
   * actually been committed (never from inside the DBIO transaction itself) - see createUnreadStatusRows
   * for what runs on the other end.
   */
  private def dispatchUnreadStatusCreation(messageId: Long, senderUserId: Long): Unit = {
    actorSystem.actorSelection(s"/user/${BulkTaskActor.actorName}") ! CreateMessageUnreadStatus(messageId, senderUserId)
  }

  private[managers] def createMessageInternal(senderUserId: Long, subject: Option[String], body: Option[String], bodyText: Option[String], important: Boolean, targetsOrRecipients: Either[List[MessageTarget], Set[Long]], parentMessageId: Option[Long]): DBIO[Long] = {
    for {
      (senderOrgId, senderCommunityId, senderCommunityFullName, senderOrgIsAdmin, senderOrgFullName, senderUserName) <- PersistenceSchema.users
        .join(PersistenceSchema.organizations).on(_.organization === _.id)
        .join(PersistenceSchema.communities).on(_._2.community === _.id)
        .filter(_._1._1.id === senderUserId)
        .map(x => (x._1._2.id, x._1._2.community, x._2.fullname, x._1._2.adminOrganization, x._1._2.fullname, x._1._1.name))
        .result
        .head
      recipientOrgIds <- targetsOrRecipients match {
        case Left(targets) => resolveTargets(senderOrgId, senderCommunityId, targets)
        case Right(ids) => DBIO.successful(ids)
      }
      recipientOrgInfo <- PersistenceSchema.organizations
        .join(PersistenceSchema.communities).on(_.community === _.id)
        .filter(_._1.id inSet recipientOrgIds)
        .map(x => (x._1.id, x._1.adminOrganization, x._1.fullname, x._2.id, x._2.fullname))
        .result
        .map { result =>
          if (result.isEmpty) throw new IllegalArgumentException("No recipients defined for message")
          result
        }
      parentThreadId <- parentMessageId match {
        case Some(pid) => PersistenceSchema.messages.filter(_.id === pid).map(_.threadId).result.headOption
        case None => DBIO.successful(None)
      }
      now = TimeUtil.getCurrentTimestamp()
      senderType = peerType(senderOrgIsAdmin, senderCommunityId)
      senderName = peerNameSnapshot(senderType, senderOrgFullName, senderCommunityFullName)
      senderUserNameToUse = Some(senderUserName).filter(_.nonEmpty)
      messageId <- {
        val recipientCount = recipientOrgInfo.size
        val (singleRecipientType, singleRecipientName) = if (recipientCount == 1) {
          val (_, isAdmin, orgName, communityId, communityName) = recipientOrgInfo.head
          val recipientType = peerType(isAdmin, communityId)
          (Some(recipientType), Some(peerNameSnapshot(recipientType, orgName, communityName)))
        } else {
          (None, None)
        }
        PersistenceSchema.insertMessage += Messages(0L, subject, body, bodyText, now, None, parentMessageId, parentThreadId.getOrElse(0L), Some(senderOrgId), senderName, Some(senderUserId), senderUserNameToUse, important, senderType, singleRecipientType, singleRecipientName, recipientCount)
      }
      _ <- if (parentThreadId.isEmpty) PersistenceSchema.messages.filter(_.id === messageId).map(_.threadId).update(messageId) else DBIO.successful(())
      _ <- {
        val communityNameById = recipientOrgInfo.map(c => c._4 -> c._5).toMap
        val recipientRows = recipientOrgInfo.map { case (orgId, isAdmin, orgFullName, communityId, _) =>
          val recipientType = peerType(isAdmin, communityId)
          MessageRecipients(0L, messageId, Some(orgId), peerNameSnapshot(recipientType, orgFullName, communityNameById.getOrElse(communityId, "")), now, None, recipientType)
        }
        if (recipientRows.isEmpty) DBIO.successful(()) else PersistenceSchema.messageRecipients ++= recipientRows
      }
    } yield messageId
  }

  /**
   * Fans out a MessageUnreadStatus row for every user of every recipient organisation of `messageId`,
   * except `excludeUserId` (the sender).
   */
  def createUnreadStatusRows(messageId: Long, excludeUserId: Long): Future[Unit] = {
    DB.run(
      PersistenceSchema.messageRecipients.filter(_.messageId === messageId).map(r => (r.id, r.recipientId)).result.flatMap { recipientRows =>
        insertUnreadStatusRows(recipientRows, excludeUserId)
      }
    )
  }

  private def insertUnreadStatusRows(recipientRows: Seq[(Long, Option[Long])], excludeUserId: Long): DBIO[Unit] = {
    val recipientOrgIds = recipientRows.flatMap(_._2).toSet
    if (recipientOrgIds.isEmpty) {
      DBIO.successful(())
    } else {
      for {
        recipientUsers <- PersistenceSchema.users
          .filter(_.organization inSet recipientOrgIds)
          .map(u => (u.id, u.organization))
          .result
        _ <- {
          val unreadRows = recipientRows.flatMap {
            case (recipientRowId, Some(recipientOrgId)) =>
              recipientUsers
                .filter { case (recipientUserId, recipientUserOrgId) =>
                  recipientUserOrgId == recipientOrgId && recipientUserId != excludeUserId
                }
                .map { case (recipientUserId, _) =>
                  MessageUnreadStatus(recipientRowId, recipientUserId)
                }
            case (_, None) =>
              Seq.empty
          }
          Logger.debug("Sent message to {} user(s) in {} organisation(s)", unreadRows.size, recipientOrgIds.size)
          if (unreadRows.isEmpty) DBIO.successful(()) else PersistenceSchema.messageUnreadStatus ++= unreadRows
        }
      } yield ()
    }
  }

  private def orgIdAndIsTestBedAdminForUser(userId: Long): DBIO[ViewerUserContext] = {
    PersistenceSchema.users
      .filter(_.id === userId)
      .map(x => (x.organization, x.role))
      .result
      .headOption
      .map { result =>
        if (result.isEmpty) throw new IllegalArgumentException("User not found")
        ViewerUserContext(result.get._1, result.get._2 == Enums.UserRole.SystemAdmin.id)
      }
  }

  def getReceivedMessages(userId: Long, page: Long, limit: Long, filterText: Option[String], showRead: Boolean, showUnread: Boolean,
                          showImportant: Boolean, deliveredAfter: Option[String], deliveredBefore: Option[String],
                          sortColumn: Option[String], sortOrder: Option[String], peerTargets: List[MessageTarget]): Future[SearchResult[ReceivedMessageListItem]] = {
    val tokens = filterText.map(_.toLowerCase.split("\\s+").filter(_.nonEmpty).toList).getOrElse(Nil)
    val likePattern = if (tokens.nonEmpty) Some(tokens.mkString("%", "%", "%")) else None
    val timestampAfter = deliveredAfter.map(TimeUtil.parseTimestamp)
    val timestampBefore = deliveredBefore.map(TimeUtil.parseTimestamp)
    DB.run(
      for {
        userContext <- orgIdAndIsTestBedAdminForUser(userId)
        peerOrgIdsOpt <- resolvePeerOrgIds(userContext.orgId, peerTargets)
        results <- {
          val queryBuilder = (forCount: Boolean) => {
            var q = PersistenceSchema.messageRecipients
              .join(PersistenceSchema.messages).on(_.messageId === _.id)
              .joinLeft(PersistenceSchema.messageUnreadStatus.filter(_.userId === userId)).on(_._1.id === _.recipientId)
              .filter(_._1._1.recipientId === userContext.orgId)
              .filter(_._1._1.deletedByRecipientAt.isEmpty)
              .filterOpt(likePattern)((row, p) => row._1._2.subject.getOrElse("").toLowerCase.like(p) || row._1._2.bodyText.getOrElse("").toLowerCase.like(p))
              .filterOpt(timestampAfter)((row, d) => row._1._1.deliveredAt >= d)
              .filterOpt(timestampBefore)((row, d) => row._1._1.deliveredAt <= d)
              .filterOpt(peerOrgIdsOpt)((row, ids) => row._1._2.senderId inSet ids)
            if (showRead != showUnread) {
              q = if (showUnread) q.filter(_._2.isDefined) else q.filter(_._2.isEmpty)
            }
            if (showImportant) {
              q = q.filter(_._1._2.important === true)
            }
            if (!forCount) {
              q = sortColumn match {
                case Some("date") => if (sortOrder.contains("asc")) q.sortBy(_._1._1.deliveredAt.asc) else q.sortBy(_._1._1.deliveredAt.desc)
                case Some("peer") => if (sortOrder.contains("asc")) q.sortBy(_._1._2.senderNameSnapshot.asc) else q.sortBy(_._1._2.senderNameSnapshot.desc)
                case _ => q
              }
            }
            q
          }
          for {
            rows <- queryBuilder(false).drop((page - 1) * limit).take(limit).result
            count <- queryBuilder(true).size.result
          } yield {
            val items = rows.map { case ((r, m), unreadOpt) =>
              val senderName = resolvePeerDisplayName(m.senderType, m.senderNameSnapshot, userContext.isTestBedAdmin)
              ReceivedMessageListItem(m.id, m.subject, previewText(m.bodyText), senderName, r.deliveredAt, m.important, unreadOpt.isEmpty, m.parentMessageId)
            }
            SearchResult(items, count)
          }
        }
      } yield results
    )
  }

  /**
   * Whether userId has any unread received message - used only for the post-login notification/menu badge.
   */
  def hasUnreadMessages(userId: Long): Future[Boolean] = {
    DB.run(PersistenceSchema.messageUnreadStatus.filter(_.userId === userId).exists.result)
  }

  /**
   * Resolves the optional "sender/recipient" search filter's descriptors to a concrete organisation id
   * set - None means "no filter applied".
   */
  private def resolvePeerOrgIds(orgId: Long, peerTargets: List[MessageTarget]): DBIO[Option[Set[Long]]] = {
    if (peerTargets.isEmpty) {
      DBIO.successful(None)
    } else {
      for {
        senderOrg <- PersistenceSchema.organizations.filter(_.id === orgId).result.head
        ids <- resolveTargets(orgId, senderOrg.community, peerTargets)
      } yield Some(ids)
    }
  }

  def getSentMessages(userId: Long, page: Long, limit: Long, filterText: Option[String], showImportant: Boolean, createdAfter: Option[String],
                      createdBefore: Option[String], sortColumn: Option[String], sortOrder: Option[String], peerTargets: List[MessageTarget]): Future[SearchResult[SentMessageListItem]] = {
    val tokens = filterText.map(_.toLowerCase.split("\\s+").filter(_.nonEmpty).toList).getOrElse(Nil)
    val likePattern = if (tokens.nonEmpty) Some(tokens.mkString("%", "%", "%")) else None
    val timestampAfter = createdAfter.map(TimeUtil.parseTimestamp)
    val timestampBefore = createdBefore.map(TimeUtil.parseTimestamp)
    DB.run(
      for {
        userContext <- orgIdAndIsTestBedAdminForUser(userId)
        // Get peer organisation IDs to filter with if needed.
        peerOrgIdsOpt <- resolvePeerOrgIds(userContext.orgId, peerTargets)
        // Get the matching messages.
        results <- {
          val queryBuilder = (forCount: Boolean) => {
            var q = PersistenceSchema.messages
              .filter(_.senderId === userContext.orgId)
              .filter(_.deletedBySenderAt.isEmpty)
              .filterOpt(likePattern)((m, p) => m.subject.getOrElse("").toLowerCase.like(p) || m.bodyText.getOrElse("").toLowerCase.like(p))
              .filterOpt(timestampAfter)((m, d) => m.createdAt >= d)
              .filterOpt(timestampBefore)((m, d) => m.createdAt <= d)
            if (showImportant) {
              q = q.filter(_.important === true)
            }
            q = peerOrgIdsOpt match {
              case Some(ids) =>
                q.join(PersistenceSchema.messageRecipients).on(_.id === _.messageId)
                  .filter(_._2.recipientId inSet ids)
                  .map(_._1)
                  .distinct
              case None => q
            }
            if (!forCount) {
              q = sortColumn match {
                case Some("date") => if (sortOrder.contains("asc")) q.sortBy(_.createdAt.asc) else q.sortBy(_.createdAt.desc)
                case Some("peer") => if (sortOrder.contains("asc")) q.sortBy(_.singleRecipientNameSnapshot.asc) else q.sortBy(_.singleRecipientNameSnapshot.desc)
                case _ => q
              }
            }
            q
          }
          for {
            results <- queryBuilder(false).drop((page - 1) * limit).take(limit).result
              .map(_.map { m =>
                val displayName = m.singleRecipientType match {
                  case Some(t) => resolvePeerDisplayName(t, m.singleRecipientNameSnapshot.getOrElse(""), userContext.isTestBedAdmin)
                  case None => ""
                }
                SentMessageListItem(m.id, m.subject, previewText(m.bodyText), displayName, m.recipientCount, m.createdAt, m.important, m.parentMessageId)
              })
            resultCount <- queryBuilder(true).size.result
          } yield SearchResult(results, resultCount)
        }
      } yield results
    )
  }

  def canAccessReceivedMessage(messageId: Long, recipientOrgId: Long): Future[Boolean] = {
    DB.run(
      PersistenceSchema.messageRecipients
        .filter(_.messageId === messageId)
        .filter(_.recipientId === recipientOrgId)
        .exists
        .result
    )
  }

  def canAccessSentMessage(messageId: Long, senderOrgId: Long): Future[Boolean] = {
    DB.run(
      PersistenceSchema.messages
        .filter(_.id === messageId)
        .filter(_.senderId === senderOrgId)
        .exists
        .result
    )
  }

  def getReceivedMessageWithChain(messageId: Long, viewerId: Long, viewerOrganisationId: Long, viewerIsCommunityAdmin: Boolean, viewerIsTestBedAdmin: Boolean): Future[Option[(ReceivedMessageDetail, List[MessageChainItem])]] = {
    DB.run(
      (for {
        messageRecipientData <- PersistenceSchema.messageRecipients
          .join(PersistenceSchema.messages).on(_.messageId === _.id)
          .filter(_._1.messageId === messageId)
          .filter(_._1.recipientId === viewerOrganisationId)
          .filter(_._1.deletedByRecipientAt.isEmpty)
          .result.headOption
        _ <- messageRecipientData match {
          case Some((r, _)) => PersistenceSchema.messageUnreadStatus.filter(_.recipientId === r.id).filter(_.userId === viewerId).delete
          case None => DBIO.successful(())
        }
        allowFlags <- loadSenderNamePermissions(viewerOrganisationId, viewerIsTestBedAdmin, viewerIsCommunityAdmin, needed = messageRecipientData.isDefined)
        messageChain <- messageRecipientData match {
          case Some((_, m)) => getMessageChainInternal(m, viewerOrganisationId, viewerId, viewerIsTestBedAdmin, viewerIsCommunityAdmin, allowFlags)
          case None => DBIO.successful(List())
        }
      } yield messageRecipientData.map { case (r, m) =>
        val senderName = resolvePeerDisplayName(m.senderType, m.senderNameSnapshot, viewerIsTestBedAdmin)
        val senderUserName = resolveSenderUserName(m.senderType, m.senderUserNameSnapshot, viewerIsTestBedAdmin, viewerIsCommunityAdmin, allowFlags)
        (ReceivedMessageDetail(m.id, m.subject, m.body, senderName, senderUserName, r.deliveredAt, m.important, m.parentMessageId), messageChain)
      }).transactionally
    )
  }

  def getSentMessageWithChain(messageId: Long, viewerId: Long, viewerOrganisationId: Long, viewerIsCommunityAdmin: Boolean, viewerIsTestBedAdmin: Boolean): Future[Option[(SentMessageDetail, List[MessageChainItem])]] = {
    DB.run(
      for {
        msgOpt <- PersistenceSchema.messages
          .filter(_.id === messageId)
          .filter(_.senderId === viewerOrganisationId)
          .filter(_.deletedBySenderAt.isEmpty)
          .result.headOption
        recipientData <- msgOpt match {
          case Some(_) =>
            PersistenceSchema.messageRecipients
              .filter(_.messageId === messageId)
              .map(r => (r.recipientType, r.recipientNameSnapshot))
              .result
              .flatMap { recipientList =>
                val recipientCount = recipientList.size
                if (recipientCount == 1) {
                  val (recipientType, recipientName) = recipientList.head
                  DBIO.successful((recipientCount, Some(resolvePeerDisplayName(recipientType, recipientName, viewerIsTestBedAdmin))))
                } else {
                  DBIO.successful((recipientCount, None))
                }
              }
          case None => DBIO.successful((0, None))
        }
        allowFlags <- loadSenderNamePermissions(viewerOrganisationId, viewerIsTestBedAdmin, viewerIsCommunityAdmin, needed = msgOpt.isDefined)
        messageChain <- msgOpt match {
          case Some(m) => getMessageChainInternal(m, viewerOrganisationId, viewerId, viewerIsTestBedAdmin, viewerIsCommunityAdmin, allowFlags)
          case None => DBIO.successful(List())
        }
      } yield {
        msgOpt match {
          case Some(m) =>
            val (recipientCount, singleRecipientName) = recipientData
            Some(SentMessageDetail(m.id, m.subject, m.body, recipientCount, singleRecipientName, m.createdAt, m.important, m.parentMessageId), messageChain)
          case None => None
        }
      }
    )
  }

  /**
   * Whether `orgId` has ever been a party to `messageId` - as sender or as recipient - regardless of
   * whether either side has since deleted it from their own listing. Deletion is display-only (it only
   * affects the table listing), so this is intentionally not filtered by `deletedBySenderAt` /
   * `deletedByRecipientAt` - used to gate chain visibility, reply-target resolution, and replying itself.
   */
  private def hasMessageAccess(messageId: Long, orgId: Long): DBIO[Boolean] = {
    PersistenceSchema.messages.filter(_.id === messageId).filter(_.senderId === orgId).exists.result.flatMap { isSender =>
      if (isSender) {
        DBIO.successful(true)
      } else {
        PersistenceSchema.messageRecipients.filter(_.messageId === messageId).filter(_.recipientId === orgId).exists.result
      }
    }
  }

  private def toMessageChainItem(message: Messages, viewerId: Long, viewerIsTestBedAdmin: Boolean, viewerIsCommunityAdmin: Boolean, allowFlags: MessageSenderVisibility): MessageChainItem = {
    val senderName = resolvePeerDisplayName(message.senderType, message.senderNameSnapshot, viewerIsTestBedAdmin)
    val senderUserName = resolveSenderUserName(message.senderType, message.senderUserNameSnapshot, viewerIsTestBedAdmin, viewerIsCommunityAdmin, allowFlags)
    val viewerIsSender = message.senderId.contains(viewerId)
    MessageChainItem(message.id, message.subject, message.body, message.createdAt, message.important, senderName, senderUserName, viewerIsSender)
  }

  @tailrec
  private def walkMessageChain(nextMessageId: Option[Long], messagesById: Map[Long, Messages], viewerId: Long,
                               viewerIsTestBedAdmin: Boolean, viewerIsCommunityAdmin: Boolean,
                               allowFlags: MessageSenderVisibility, chain: ListBuffer[MessageChainItem]): Unit = {
    nextMessageId match {
      case Some(messageId) =>
        messagesById.get(messageId) match {
          case Some(m) =>
            chain.addOne(toMessageChainItem(m, viewerId, viewerIsTestBedAdmin, viewerIsCommunityAdmin, allowFlags))
            walkMessageChain(m.parentMessageId, messagesById, viewerId, viewerIsTestBedAdmin, viewerIsCommunityAdmin, allowFlags, chain)
          case None => // Nothing to do.
        }
      case None => // Nothing to do
    }
  }

  def getMessageChain(anchorId: Long, viewerId: Long, viewerUserId: Long, viewerIsTestBedAdmin: Boolean, viewerIsCommunityAdmin: Boolean): Future[List[MessageChainItem]] = {
    DB.run(
      for {
        message <- PersistenceSchema.messages.filter(_.id === anchorId).result.headOption
        allowFlags <- loadSenderNamePermissions(viewerId, viewerIsTestBedAdmin = viewerIsTestBedAdmin, viewerIsCommunityAdmin = viewerIsCommunityAdmin, needed = message.isDefined)
        messageChain <- if (message.isDefined) {
          getMessageChainInternal(message.get, viewerId, viewerUserId, viewerIsTestBedAdmin, viewerIsCommunityAdmin, allowFlags)
        } else DBIO.successful(List())
      } yield {
        messageChain.prepended(toMessageChainItem(message.get, viewerId, viewerIsTestBedAdmin, viewerIsCommunityAdmin, allowFlags))
      }
    )
  }

  /**
   * The chain of ancestor messages leading up to (and including) `anchor`, oldest first - used both to
   * show "earlier messages" below a reply's body in the compose modal (anchor = the message being
   * replied to) and to show them above a reply's own content in the message detail panel (anchor = that
   * message's parentMessageId).
   */
  def getMessageChainInternal(anchor: Messages, viewerId: Long, viewerUserId: Long, viewerIsTestBedAdmin: Boolean, viewerIsCommunityAdmin: Boolean, allowFlags: MessageSenderVisibility): DBIO[List[MessageChainItem]] = {
    for {
      messagesInChain <- PersistenceSchema.messages
        .filter(_.threadId === anchor.threadId)
        .filter(_.createdAt <= anchor.createdAt)
        .filter(_.id =!= anchor.id)
        .result
    } yield {
      val messagesById = messagesInChain.map(m => m.id -> m).toMap
      val orderedMessages = ListBuffer[MessageChainItem]()
      walkMessageChain(anchor.parentMessageId, messagesById, viewerId, viewerIsTestBedAdmin, viewerIsCommunityAdmin, allowFlags, orderedMessages)
      orderedMessages.toList
    }
  }

  private def buildReplyTargetInfo(targetOrg: Organizations, replierOrgId: Long, replierIsAdmin: Boolean, replierCommunityId: Long): DBIO[Option[ReplyTargetInfo]] = {
    val replierIsTestBedAdmin = replierIsAdmin && replierCommunityId == Constants.DefaultCommunityId
    val replierIsCommunityAdmin = replierIsAdmin && !replierIsTestBedAdmin
    if (targetOrg.id == replierOrgId) {
      // Self-reply: your own organisation's self-referential admin target.
      if (replierIsTestBedAdmin) DBIO.successful(Some(ReplyTargetInfo(Some(MessageTargetType.TestBedAdmin.id.toShort), None, None, None, None)))
      else if (replierIsCommunityAdmin) DBIO.successful(Some(ReplyTargetInfo(Some(MessageTargetType.CommunityAdmin.id.toShort), None, None, None, None)))
      else DBIO.successful(Some(ReplyTargetInfo(Some(MessageTargetType.OwnOrganisation.id.toShort), None, None, None, None)))
    } else if (targetOrg.adminOrganization && targetOrg.community == Constants.DefaultCommunityId) {
      // Sender is the Test Bed admin org.
      if (replierIsTestBedAdmin || replierIsCommunityAdmin) {
        DBIO.successful(Some(ReplyTargetInfo(Some(MessageTargetType.TestBedAdmin.id.toShort), None, None, None, None)))
      } else {
        // Organisation user: policy exception - default to their own community's admin, not the Test Bed admin.
        DBIO.successful(Some(ReplyTargetInfo(Some(MessageTargetType.CommunityAdmin.id.toShort), None, None, None, None)))
      }
    } else if (targetOrg.adminOrganization) {
      // Sender is a real community's admin org (not the replier's own - handled above).
      if (replierIsTestBedAdmin) {
        PersistenceSchema.communities.filter(_.id === targetOrg.community).result.headOption.map { communityOpt =>
          Some(ReplyTargetInfo(Some(MessageTargetType.CommunityAdmin.id.toShort), Some(targetOrg.community), communityOpt.map(_.fullname), None, None))
        }
      } else {
        // Organisation user (necessarily a member of that same community).
        DBIO.successful(Some(ReplyTargetInfo(Some(MessageTargetType.CommunityAdmin.id.toShort), None, None, None, None)))
      }
    } else {
      // Sender is a plain organisation - only reachable when the replier is that community's admin, or the Test Bed admin.
      val communityIdOpt = if (replierIsTestBedAdmin) Some(targetOrg.community) else None
      PersistenceSchema.communities.filter(_.id === targetOrg.community).result.headOption.map { communityOpt =>
        Some(ReplyTargetInfo(Some(MessageTargetType.Organisation.id.toShort), communityIdOpt, if (replierIsTestBedAdmin) communityOpt.map(_.fullname) else None, Some(targetOrg.id), Some(targetOrg.fullname)))
      }
    }
  }

  def resolveReplyTarget(userId: Long, parentMessageId: Long): Future[Option[ReplyTargetInfo]] = {
    DB.run(
      for {
        replierOrgId <- PersistenceSchema.users
          .filter(_.id === userId)
          .map(_.organization)
          .result
          .headOption
          .map {
            case Some(orgId) => orgId
            case None => throw new IllegalArgumentException("User not found")
          }
        hasAccess <- hasMessageAccess(parentMessageId, replierOrgId)
        parentOpt <- if (hasAccess) PersistenceSchema.messages.filter(_.id === parentMessageId).result.headOption else DBIO.successful(None: Option[Messages])
        targetOrgOpt <- parentOpt.flatMap(_.senderId) match {
          case Some(id) => PersistenceSchema.organizations.filter(_.id === id).result.headOption
          case None => DBIO.successful(None: Option[Organizations])
        }
        replierOrgOpt <- PersistenceSchema.organizations.filter(_.id === replierOrgId).result.headOption
        result <- (targetOrgOpt, replierOrgOpt) match {
          case (Some(targetOrg), Some(replierOrg)) => buildReplyTargetInfo(targetOrg, replierOrgId, replierOrg.adminOrganization, replierOrg.community)
          case _ => DBIO.successful(None: Option[ReplyTargetInfo])
        }
      } yield result
    )
  }

  def getMessageRecipientNames(userId: Long, messageId: Long): Future[List[String]] = {
    DB.run(
      for {
        userContext <- orgIdAndIsTestBedAdminForUser(userId)
        owns <- PersistenceSchema.messages.filter(_.id === messageId).filter(_.senderId === userContext.orgId).exists.result
        result <- {
          if (owns) {
            PersistenceSchema.messageRecipients
              .filter(_.messageId === messageId)
              .map(r => (r.recipientType, r.recipientNameSnapshot))
              .result
              .map { recipients =>
              recipients.map { case (recipientType, snapshot) => resolvePeerDisplayName(recipientType, snapshot, userContext.isTestBedAdmin) }.sorted.toList
            }
          } else {
            DBIO.successful(Nil)
          }
        }
      } yield result
    )
  }

  /**
   * Marking read deletes the user's unread rows for the affected recipient deliveries; marking unread
   * (re-)inserts them, skipping any recipient delivery that is already unread for this user to respect
   * MessageUnreadStatus's primary key.
   */
  def markReceivedMessagesRead(ids: List[Long], read: Boolean, userId: Long): Future[Unit] = {
    DB.run(
      (for {
        orgId <- PersistenceSchema.users.filter(_.id === userId).map(_.organization).result.head
        recipientRowIds <- PersistenceSchema.messageRecipients.filter(_.messageId inSet ids).filter(_.recipientId === orgId).map(_.id).result
        _ <- if (recipientRowIds.isEmpty) {
          DBIO.successful(())
        } else if (read) {
          PersistenceSchema.messageUnreadStatus.filter(_.userId === userId).filter(_.recipientId inSet recipientRowIds).delete
        } else {
          PersistenceSchema.messageUnreadStatus.filter(_.userId === userId).filter(_.recipientId inSet recipientRowIds).map(_.recipientId).result.flatMap { alreadyUnread =>
            val alreadyUnreadIds = alreadyUnread.toSet
            val toInsert = recipientRowIds.filterNot(alreadyUnreadIds.contains).map(recipientRowId => MessageUnreadStatus(recipientRowId, userId))
            if (toInsert.isEmpty) DBIO.successful(()) else PersistenceSchema.messageUnreadStatus ++= toInsert
          }
        }
      } yield ()).transactionally
    )
  }

  /**
   * Deletion is organisation-wide, so the unread rows for every user of the recipient
   * organisation are cleared along with it - otherwise a message nobody in the organisation can open any
   * more would keep flagging the unread badge.
   */
  def deleteReceivedMessages(ids: List[Long], userId: Long): Future[Unit] = {
    DB.run(
      (for {
        orgId <- PersistenceSchema.users.filter(_.id === userId).map(_.organization).result.head
        recipientRowIds <- PersistenceSchema.messageRecipients.filter(_.messageId inSet ids).filter(_.recipientId === orgId).map(_.id).result
        _ <- PersistenceSchema.messageRecipients
          .filter(_.messageId inSet ids)
          .filter(_.recipientId === orgId)
          .map(_.deletedByRecipientAt)
          .update(Some(TimeUtil.getCurrentTimestamp()))
        _ <- if (recipientRowIds.isEmpty) DBIO.successful(()) else PersistenceSchema.messageUnreadStatus.filter(_.recipientId inSet recipientRowIds).delete
      } yield ()).transactionally
    )
  }

  def deleteSentMessages(ids: List[Long], userId: Long): Future[Unit] = {
    DB.run(
      (for {
        orgId <- PersistenceSchema.users.filter(_.id === userId).map(_.organization).result.head
        _ <- PersistenceSchema.messages
          .filter(_.id inSet ids)
          .filter(_.senderId === orgId)
          .map(_.deletedBySenderAt)
          .update(Some(TimeUtil.getCurrentTimestamp()))
      } yield ()).transactionally
    )
  }

  /**
   * Called when an organisation is deleted. Must run before the organisation row itself is deleted.
   */
  private[managers] def clearOrganisationReferences(orgId: Long): DBIO[_] = {
    for {
      _ <- PersistenceSchema.messages.filter(_.senderId === orgId).map(_.senderId).update(None)
      _ <- PersistenceSchema.messageRecipients.filter(_.recipientId === orgId).map(_.recipientId).update(None)
    } yield ()
  }

  /**
   * Called when one or more users are (hard-)deleted.
   */
  private[managers] def clearUserReferences(userIds: Seq[Long]): DBIO[_] = {
    if (userIds.isEmpty) {
      DBIO.successful(())
    } else {
      for {
        _ <- PersistenceSchema.messages.filter(_.senderUserId inSet userIds).map(m => (m.senderUserId, m.senderUserNameSnapshot)).update((None, None))
        _ <- PersistenceSchema.messageUnreadStatus.filter(_.userId inSet userIds).delete
      } yield ()
    }
  }

}
