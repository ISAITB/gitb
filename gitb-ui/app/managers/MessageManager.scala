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

import models.Enums.MessageTargetType
import models._
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{HtmlUtil, TimeUtil}

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Manages the internal (organisation-to-organisation) messaging feature: messages and their per-
 * recipient delivery/read state ("My messages" screen). See MessageService for the REST endpoints and
 * AuthorizationManager for the send-time recipient authorisation.
 */
@Singleton
class MessageManager @Inject() (dbConfigProvider: DatabaseConfigProvider)
                                (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  /** Cap applied to the body preview shown in the message listing, to keep list payloads bounded. */
  private val PreviewMaxLength = 300

  private def previewText(bodyText: Option[String]): Option[String] = {
    bodyText.map { text =>
      if (text.length > PreviewMaxLength) text.take(PreviewMaxLength) + "…" else text
    }
  }

  /** The name recorded as the sender/recipient snapshot for an organisation. Non-admin organisations use
   * their own full name. A community's admin organisation uses "<community full name> administrator",
   * except the default community's, which is "Test Bed administrator". */
  private def displayName(org: Organizations, communityFullName: String): String = {
    val name =
      if (!org.adminOrganization) {
        org.fullname
      } else if (org.community == Constants.DefaultCommunityId) {
        "Test Bed administrator"
      } else {
        s"$communityFullName administrator"
      }
    if (name.length > 254) name.take(254) else name
  }

  /** Resolves the *viewer*-facing display name override for any of `orgIds` that is a still-existing
   * admin organisation of a real (non-default) community - i.e. the same admin organisation is labelled
   * differently depending on who's looking at it, rather than using the frozen send-time snapshot:
   * generically as "Community administrator" for an organisation user or community admin viewer, or as
   * "Administrator of &lt;community full name&gt;" for a Test Bed admin viewer. Non-admin organisations, the
   * Test Bed's own default-community admin organisation (still "Test Bed administrator" for everyone),
   * and deleted organisations (absent from `orgIds` by construction, since callers only pass ids of
   * organisations that still exist) are intentionally not covered here - they keep using the existing
   * snapshot text unchanged. */
  private def resolveAdminPeerNames(orgIds: Set[Long], viewerIsTestBedAdmin: Boolean): DBIO[Map[Long, String]] = {
    if (orgIds.isEmpty) {
      DBIO.successful(Map.empty)
    } else {
      PersistenceSchema.organizations
        .join(PersistenceSchema.communities).on(_.community === _.id)
        .filter(_._1.id inSet orgIds)
        .filter(_._1.adminOrganization === true)
        .filter(_._1.community =!= Constants.DefaultCommunityId)
        .map(x => (x._1.id, x._2.fullname))
        .result
        .map(_.map { case (orgId, communityFullname) =>
          orgId -> (if (viewerIsTestBedAdmin) s"Administrator of $communityFullname" else "Community administrator")
        }.toMap)
    }
  }

  /** Resolves a recipient descriptor to a set of organisation ids. Structural/role authorisation of the
   * descriptors themselves is done separately by AuthorizationManager.canSendMessage before this is
   * called - this only expands group targets into concrete organisation ids. */
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
      }
    }
    DBIO.sequence(actions).map(_.flatten.toSet)
  }

  private def prepareBody(rawBody: Option[String]): (Option[String], Option[String]) = {
    val sanitizedBody = rawBody.map(HtmlUtil.sanitizeMinimalEditorContent).filter(_.nonEmpty)
    val bodyText = sanitizedBody.map(HtmlUtil.toPlainText).filter(_.nonEmpty)
    (sanitizedBody, bodyText)
  }

  def createMessage(senderOrgId: Long, senderUserId: Long, subject: Option[String], rawBody: Option[String], important: Boolean, targets: List[MessageTarget]): Future[Long] = {
    val (sanitizedBody, bodyText) = prepareBody(rawBody)
    DB.run(createMessageInternal(senderOrgId, senderUserId, subject.filter(_.nonEmpty), sanitizedBody, bodyText, important, Left(targets), None).transactionally)
  }

  /** Unlike a new message, a reply requires the sending organisation to have been a party (sender or
   * recipient) to the message being replied to - see hasMessageAccess. Beyond that gate, the recipient
   * is fully user-chosen and resolved/authorised exactly like a new message (see resolveTargets and
   * AuthorizationManager.canSendMessage, called by the controller before this is called) - the client
   * only pre-selects a sensible default via resolveReplyTarget, it does not lock the recipient. */
  def createMessageReply(senderOrgId: Long, senderUserId: Long, parentMessageId: Long, subject: Option[String], rawBody: Option[String], important: Boolean, targets: List[MessageTarget]): Future[Long] = {
    val (sanitizedBody, bodyText) = prepareBody(rawBody)
    DB.run(
      (for {
        hasAccess <- hasMessageAccess(parentMessageId, senderOrgId)
        messageId <- if (hasAccess) {
          createMessageInternal(senderOrgId, senderUserId, subject.filter(_.nonEmpty), sanitizedBody, bodyText, important, Left(targets), Some(parentMessageId))
        } else {
          DBIO.failed(new IllegalArgumentException("The message being replied to could not be found."))
        }
      } yield messageId).transactionally
    )
  }

  private[managers] def createMessageInternal(senderOrgId: Long, senderUserId: Long, subject: Option[String], body: Option[String], bodyText: Option[String], important: Boolean, targetsOrRecipients: Either[List[MessageTarget], Set[Long]], parentMessageId: Option[Long]): DBIO[Long] = {
    for {
      senderOrg <- PersistenceSchema.organizations.filter(_.id === senderOrgId).result.head
      senderCommunity <- PersistenceSchema.communities.filter(_.id === senderOrg.community).result.head
      recipientOrgIds <- targetsOrRecipients match {
        case Left(targets) => resolveTargets(senderOrgId, senderOrg.community, targets)
        case Right(ids) => DBIO.successful(ids)
      }
      recipientOrgs <- PersistenceSchema.organizations.filter(_.id inSet recipientOrgIds).result
      recipientCommunities <- PersistenceSchema.communities.filter(_.id inSet recipientOrgs.map(_.community).toSet).result
      parentThreadId <- parentMessageId match {
        case Some(pid) => PersistenceSchema.messages.filter(_.id === pid).map(_.threadId).result.headOption
        case None => DBIO.successful(None)
      }
      now = TimeUtil.getCurrentTimestamp()
      senderName = displayName(senderOrg, senderCommunity.fullname)
      messageId <- PersistenceSchema.insertMessage += Messages(0L, subject, body, bodyText, now, None, parentMessageId, parentThreadId.getOrElse(0L), Some(senderOrgId), senderName, Some(senderUserId), important)
      _ <- if (parentThreadId.isEmpty) PersistenceSchema.messages.filter(_.id === messageId).map(_.threadId).update(messageId) else DBIO.successful(())
      _ <- {
        val communityNameById = recipientCommunities.map(c => c.id -> c.fullname).toMap
        val recipientRows = recipientOrgs.map { org =>
          MessageRecipients(0L, messageId, Some(org.id), displayName(org, communityNameById.getOrElse(org.community, "")), now, None, None)
        }
        if (recipientRows.isEmpty) DBIO.successful(()) else PersistenceSchema.messageRecipients ++= recipientRows
      }
    } yield messageId
  }

  def getReceivedMessages(orgId: Long, page: Long, limit: Long, filterText: Option[String], showRead: Boolean, showUnread: Boolean,
                          showImportant: Boolean, deliveredAfter: Option[String], deliveredBefore: Option[String],
                          sortColumn: Option[String], sortOrder: Option[String], peerTargets: List[MessageTarget], viewerIsTestBedAdmin: Boolean): Future[SearchResult[ReceivedMessageListItem]] = {
    val tokens = filterText.map(_.toLowerCase.split("\\s+").filter(_.nonEmpty).toList).getOrElse(Nil)
    val likePattern = if (tokens.nonEmpty) Some(tokens.mkString("%", "%", "%")) else None
    resolvePeerOrgIds(orgId, peerTargets).flatMap { peerOrgIdsOpt =>
      val queryBuilder = (forCount: Boolean) => {
        var q = PersistenceSchema.messageRecipients
          .join(PersistenceSchema.messages).on(_.messageId === _.id)
          .filter(_._1.recipientId === orgId)
          .filter(_._1.deletedByRecipientAt.isEmpty)
          .filterOpt(likePattern)((row, p) => row._2.subject.getOrElse("").toLowerCase.like(p) || row._2.bodyText.getOrElse("").toLowerCase.like(p))
          .filterOpt(deliveredAfter)((row, d) => row._1.deliveredAt >= TimeUtil.parseTimestamp(d))
          .filterOpt(deliveredBefore)((row, d) => row._1.deliveredAt <= TimeUtil.parseTimestamp(d))
          .filterOpt(peerOrgIdsOpt)((row, ids) => row._2.senderId inSet ids)
        if (showRead != showUnread) {
          q = if (showRead) q.filter(_._1.readAt.isDefined) else q.filter(_._1.readAt.isEmpty)
        }
        if (showImportant) {
          q = q.filter(_._2.important === true)
        }
        if (!forCount) {
          q = sortColumn.getOrElse("date") match {
            case "peer" => if (sortOrder.contains("asc")) q.sortBy(_._2.senderNameSnapshot.asc) else q.sortBy(_._2.senderNameSnapshot.desc)
            case _ => if (sortOrder.contains("asc")) q.sortBy(_._1.deliveredAt.asc) else q.sortBy(_._1.deliveredAt.desc)
          }
        }
        q
      }
      DB.run(
        for {
          rows <- queryBuilder(false).drop((page - 1) * limit).take(limit).result
          count <- queryBuilder(true).size.result
          adminNames <- resolveAdminPeerNames(rows.flatMap(_._2.senderId).toSet, viewerIsTestBedAdmin)
        } yield {
          val items = rows.map { case (r, m) =>
            val senderName = m.senderId.flatMap(adminNames.get).getOrElse(m.senderNameSnapshot)
            ReceivedMessageListItem(m.id, m.subject, previewText(m.bodyText), senderName, r.deliveredAt, m.important, r.readAt.isDefined, m.parentMessageId)
          }
          SearchResult(items, count)
        }
      )
    }
  }

  /** Whether the requesting organisation has any unread received message that wasn't sent by the
   * requesting user herself - used only for the post-login notification/menu badge (see
   * HealthCheckService's post-login check for the analogous service-health precedent), a single cheap
   * `exists` over the same join getReceivedMessages already uses. */
  def hasUnreadMessagesFromOthers(orgId: Long, userId: Long): Future[Boolean] = {
    DB.run(
      PersistenceSchema.messageRecipients
        .join(PersistenceSchema.messages).on(_.messageId === _.id)
        .filter(_._1.recipientId === orgId)
        .filter(_._1.deletedByRecipientAt.isEmpty)
        .filter(_._1.readAt.isEmpty)
        // getOrElse keeps this a plain comparison for a sender-less (deleted user) row, which then
        // simply counts as unread (not sent by the viewing user).
        .filter(_._2.senderUserId.getOrElse(-1L) =!= userId)
        .exists.result
    )
  }

  /** Resolves the optional "sender/recipient" search filter's descriptors to a concrete organisation id
   * set - None means "no filter applied" (the descriptor list was empty), as distinct from Some(Set())
   * which matches nothing (e.g. a community with no admin organisation). */
  private def resolvePeerOrgIds(orgId: Long, peerTargets: List[MessageTarget]): Future[Option[Set[Long]]] = {
    if (peerTargets.isEmpty) {
      Future.successful(None)
    } else {
      DB.run(
        for {
          senderOrg <- PersistenceSchema.organizations.filter(_.id === orgId).result.head
          ids <- resolveTargets(orgId, senderOrg.community, peerTargets)
        } yield ids
      ).map(Some(_))
    }
  }

  def getSentMessages(orgId: Long, page: Long, limit: Long, filterText: Option[String], showImportant: Boolean, createdAfter: Option[String],
                      createdBefore: Option[String], sortColumn: Option[String], sortOrder: Option[String], peerTargets: List[MessageTarget], viewerIsTestBedAdmin: Boolean): Future[SearchResult[SentMessageListItem]] = {
    val tokens = filterText.map(_.toLowerCase.split("\\s+").filter(_.nonEmpty).toList).getOrElse(Nil)
    val likePattern = if (tokens.nonEmpty) Some(tokens.mkString("%", "%", "%")) else None
    resolvePeerOrgIds(orgId, peerTargets).flatMap { peerOrgIdsOpt =>
      var q = PersistenceSchema.messages
        .filter(_.senderId === orgId)
        .filter(_.deletedBySenderAt.isEmpty)
        .filterOpt(likePattern)((m, p) => m.subject.getOrElse("").toLowerCase.like(p) || m.bodyText.getOrElse("").toLowerCase.like(p))
        .filterOpt(createdAfter)((m, d) => m.createdAt >= TimeUtil.parseTimestamp(d))
        .filterOpt(createdBefore)((m, d) => m.createdAt <= TimeUtil.parseTimestamp(d))
      if (showImportant) {
        q = q.filter(_.important === true)
      }
      q = peerOrgIdsOpt match {
        case Some(ids) => q.filter(m => PersistenceSchema.messageRecipients.filter(r => r.messageId === m.id && (r.recipientId inSet ids)).exists)
        case None => q
      }
      DB.run(
        for {
          idsAndDates <- q.map(m => (m.id, m.createdAt)).result
          recipientInfo <- PersistenceSchema.messageRecipients.filter(_.messageId inSet idsAndDates.map(_._1)).map(r => (r.messageId, r.recipientId, r.recipientNameSnapshot)).result
          adminNames <- resolveAdminPeerNames(recipientInfo.flatMap(_._2).toSet, viewerIsTestBedAdmin)
        } yield {
          val resolvedRecipientInfo = recipientInfo.map { case (messageId, recipientId, snapshot) =>
            (messageId, recipientId.flatMap(adminNames.get).getOrElse(snapshot))
          }
          val grouped = resolvedRecipientInfo.groupBy(_._1).view.mapValues(_.map(_._2).sorted.toList).toMap
          def peerOf(id: Long): String = grouped.getOrElse(id, Nil).headOption.getOrElse("")
          val ordered = sortColumn.getOrElse("date") match {
            case "peer" =>
              val sorted = idsAndDates.sortBy { case (id, _) => peerOf(id) }
              if (sortOrder.contains("desc")) sorted.reverse else sorted
            case _ =>
              val sorted = idsAndDates.sortBy(_._2.getTime)
              if (sortOrder.contains("asc")) sorted else sorted.reverse
          }
          (ordered, grouped)
        }
      )
    }.flatMap { case (ordered, grouped) =>
      val count = ordered.size
      val pageIds = ordered.slice(((page - 1) * limit).toInt, ((page - 1) * limit + limit).toInt).map(_._1)
      if (pageIds.isEmpty) {
        Future.successful(SearchResult[SentMessageListItem](Seq.empty, count))
      } else {
        DB.run(PersistenceSchema.messages.filter(_.id inSet pageIds).result).map { msgs =>
          val byId = msgs.map(m => m.id -> m).toMap
          val items = pageIds.flatMap(byId.get).map { m =>
            val names = grouped.getOrElse(m.id, Nil)
            SentMessageListItem(m.id, m.subject, previewText(m.bodyText), names.headOption.getOrElse(""), names.size, m.createdAt, m.important, m.parentMessageId)
          }
          SearchResult(items, count)
        }
      }
    }
  }

  /** Fetches a received message's detail and, if it was unread, marks it as read - both in one
   * transaction. Scoped to the requesting organisation: returns None if the message doesn't exist, isn't
   * addressed to this organisation, or was deleted by this organisation's recipient. */
  def getReceivedMessageDetail(messageId: Long, orgId: Long, viewerIsTestBedAdmin: Boolean): Future[Option[ReceivedMessageDetail]] = {
    DB.run(
      (for {
        rowOpt <- PersistenceSchema.messageRecipients
          .join(PersistenceSchema.messages).on(_.messageId === _.id)
          .filter(_._1.messageId === messageId)
          .filter(_._1.recipientId === orgId)
          .filter(_._1.deletedByRecipientAt.isEmpty)
          .result.headOption
        _ <- rowOpt match {
          case Some((r, _)) if r.readAt.isEmpty =>
            PersistenceSchema.messageRecipients.filter(_.id === r.id).map(_.readAt).update(Some(TimeUtil.getCurrentTimestamp()))
          case _ => DBIO.successful(())
        }
        adminNames <- resolveAdminPeerNames(rowOpt.flatMap(_._2.senderId).toSet, viewerIsTestBedAdmin)
      } yield rowOpt.map { case (r, m) =>
        val senderName = m.senderId.flatMap(adminNames.get).getOrElse(m.senderNameSnapshot)
        ReceivedMessageDetail(m.id, m.subject, m.body, senderName, r.deliveredAt, m.important, m.parentMessageId)
      }).transactionally
    )
  }

  /** Scoped to the requesting organisation as the sender: returns None if the message doesn't exist,
   * wasn't sent by this organisation, or was deleted by its sender. */
  def getSentMessageDetail(messageId: Long, orgId: Long, viewerIsTestBedAdmin: Boolean): Future[Option[SentMessageDetail]] = {
    DB.run(
      for {
        msgOpt <- PersistenceSchema.messages.filter(_.id === messageId).filter(_.senderId === orgId).filter(_.deletedBySenderAt.isEmpty).result.headOption
        count <- if (msgOpt.isDefined) PersistenceSchema.messageRecipients.filter(_.messageId === messageId).length.result else DBIO.successful(0)
        singleRecipient <- if (msgOpt.isDefined && count == 1) {
          PersistenceSchema.messageRecipients.filter(_.messageId === messageId).map(r => (r.recipientId, r.recipientNameSnapshot)).result.headOption
        } else {
          DBIO.successful(None)
        }
        adminNames <- resolveAdminPeerNames(singleRecipient.flatMap(_._1).toSet, viewerIsTestBedAdmin)
      } yield {
        val singleName = singleRecipient.map { case (recipientId, snapshot) => recipientId.flatMap(adminNames.get).getOrElse(snapshot) }
        msgOpt.map(m => SentMessageDetail(m.id, m.subject, m.body, count, singleName, m.createdAt, m.important, m.parentMessageId))
      }
    )
  }

  /** Whether `orgId` has ever been a party to `messageId` - as sender or as recipient - regardless of
   * whether either side has since deleted it from their own listing. Deletion is display-only (it only
   * affects the table listing), so this is intentionally not filtered by `deletedBySenderAt` /
   * `deletedByRecipientAt` - used to gate chain visibility, reply-target resolution, and replying itself. */
  private def hasMessageAccess(messageId: Long, orgId: Long): DBIO[Boolean] = {
    PersistenceSchema.messages.filter(_.id === messageId).filter(_.senderId === orgId).exists.result.flatMap { isSender =>
      if (isSender) {
        DBIO.successful(true)
      } else {
        PersistenceSchema.messageRecipients.filter(_.messageId === messageId).filter(_.recipientId === orgId).exists.result
      }
    }
  }

  /** The chain of ancestor messages leading up to (and including) `anchorId`, oldest first - used both to
   * show "earlier messages" below a reply's body in the compose modal (anchorId = the message being
   * replied to) and to show them above a reply's own content in the message detail panel (anchorId = that
   * message's parentMessageId). The walk stops (silently truncating) at the first ancestor the requesting
   * organisation was never a sender or recipient of - see hasMessageAccess (deletion does not truncate
   * it).
   *
   * Loads every candidate ancestor with a single query rather than walking parentMessageId one DB round
   * trip at a time: all of anchorId's ancestors necessarily share its threadId (set on insert - see
   * createMessageInternal) and were created at or before it, so that's fetched in one shot, along with a
   * single batched recipient-access check for the whole candidate set (replacing a per-ancestor
   * hasMessageAccess call). The actual chain - as opposed to every earlier message in the thread - is
   * then reconstructed by walking parentMessageId in memory against the fetched candidates. */
  def getMessageChain(anchorId: Long, orgId: Long, viewerIsTestBedAdmin: Boolean): Future[List[MessageChainItem]] = {
    DB.run(
      PersistenceSchema.messages.filter(_.id === anchorId).result.headOption.flatMap {
        case None => DBIO.successful((Option.empty[Messages], Map.empty[Long, Messages], Set.empty[Long]))
        case Some(anchor) =>
          for {
            // createdAt <= anchor.createdAt (not <) so an ancestor sharing the anchor's exact timestamp
            // is not missed; id =!= anchorId since the anchor itself is already in hand.
            candidates <- PersistenceSchema.messages
              .filter(_.threadId === anchor.threadId)
              .filter(_.createdAt <= anchor.createdAt)
              .filter(_.id =!= anchorId)
              .result
            recipientAccessibleIds <- PersistenceSchema.messageRecipients
              .filter(_.messageId inSet (anchorId +: candidates.map(_.id)).toSet)
              .filter(_.recipientId === orgId)
              .map(_.messageId).result
          } yield (Some(anchor), candidates.map(m => m.id -> m).toMap, recipientAccessibleIds.toSet)
      }
    ).flatMap { case (anchorOpt, byId, recipientAccessibleIds) =>
      // Sender-side access is decided from each row's own senderId; recipient-side access was already
      // batched above.
      def isAccessible(m: Messages): Boolean = m.senderId.contains(orgId) || recipientAccessibleIds.contains(m.id)
      def walk(current: Option[Messages], acc: List[Messages]): List[Messages] = current match {
        case Some(m) if isAccessible(m) => walk(m.parentMessageId.flatMap(byId.get), m :: acc)
        case _ => acc
      }
      val entries = walk(anchorOpt, Nil)
      DB.run(resolveAdminPeerNames(entries.flatMap(_.senderId).toSet, viewerIsTestBedAdmin)).map { adminNames =>
        entries.map { m =>
          val senderName = m.senderId.flatMap(adminNames.get).getOrElse(m.senderNameSnapshot)
          MessageChainItem(m.id, m.subject, previewText(m.bodyText), m.body, m.createdAt, m.important, senderName)
        }
      }
    }
  }

  /** The default recipient a reply's picker should be pre-selected with, expressed as a target
   * descriptor rather than a resolved organisation - so the caller can seed the *same* role-specific
   * picker used for a new message. None means no sensible default could be resolved (the requesting
   * organisation was never a party to the parent message, or the parent's sender organisation has since
   * been deleted) - the picker then simply starts empty.
   *
   * The rule (confirmed with the feature owner): replying to your own sent message defaults to your own
   * "self" admin target (Test Bed administrator / Community administrator / My organisation, whichever
   * applies to the replying role) rather than looping back to yourself as a plain organisation target.
   * Replying to a received message defaults to whoever sent it, expressed in a form the replying role is
   * actually allowed to address - notably, an organisation user replying to a message from the Test Bed
   * administrator defaults to "Community administrator" (their own community), since organisation users
   * are not permitted to address the Test Bed administrator directly (see AuthorizationManager.canSendMessage). */
  // Extracted with an explicit DBIO[...] return type (rather than inlined as a for-comprehension
  // generator) - Slick's DBIOAction has an effect-type parameter that Scala cannot always unify
  // structurally across match branches mixing DBIO.successful (no read effect) with a real
  // query's .map (a read effect); an explicit declared return type resolves it.
  private def buildReplyTargetInfo(targetOrg: Organizations, replierOrgId: Long, replierIsAdmin: Boolean, replierCommunityId: Long): DBIO[Option[ReplyTargetInfo]] = {
    val replierIsTestBedAdmin = replierIsAdmin && replierCommunityId == Constants.DefaultCommunityId
    val replierIsCommunityAdmin = replierIsAdmin && !replierIsTestBedAdmin
    def fixed(info: ReplyTargetInfo): DBIO[Option[ReplyTargetInfo]] = DBIO.successful(Option(info))
    if (targetOrg.id == replierOrgId) {
      // Self-reply: your own organisation's self-referential admin target.
      if (replierIsTestBedAdmin) fixed(ReplyTargetInfo(Some(MessageTargetType.TestBedAdmin.id.toShort), None, None, None, None))
      else if (replierIsCommunityAdmin) fixed(ReplyTargetInfo(Some(MessageTargetType.CommunityAdmin.id.toShort), None, None, None, None))
      else fixed(ReplyTargetInfo(Some(MessageTargetType.OwnOrganisation.id.toShort), None, None, None, None))
    } else if (targetOrg.adminOrganization && targetOrg.community == Constants.DefaultCommunityId) {
      // Sender is the Test Bed admin org.
      if (replierIsTestBedAdmin || replierIsCommunityAdmin) {
        fixed(ReplyTargetInfo(Some(MessageTargetType.TestBedAdmin.id.toShort), None, None, None, None))
      } else {
        // Organisation user: policy exception - default to their own community's admin, not the Test Bed admin.
        fixed(ReplyTargetInfo(Some(MessageTargetType.CommunityAdmin.id.toShort), None, None, None, None))
      }
    } else if (targetOrg.adminOrganization) {
      // Sender is a real community's admin org (not the replier's own - handled above).
      if (replierIsTestBedAdmin) {
        PersistenceSchema.communities.filter(_.id === targetOrg.community).result.headOption.map { communityOpt =>
          Option(ReplyTargetInfo(Some(MessageTargetType.CommunityAdmin.id.toShort), Some(targetOrg.community), communityOpt.map(_.fullname), None, None))
        }
      } else {
        // Organisation user (necessarily a member of that same community).
        fixed(ReplyTargetInfo(Some(MessageTargetType.CommunityAdmin.id.toShort), None, None, None, None))
      }
    } else {
      // Sender is a plain organisation - only reachable when the replier is that community's admin, or the Test Bed admin.
      val communityIdOpt = if (replierIsTestBedAdmin) Some(targetOrg.community) else None
      PersistenceSchema.communities.filter(_.id === targetOrg.community).result.headOption.map { communityOpt =>
        Option(ReplyTargetInfo(Some(MessageTargetType.Organisation.id.toShort), communityIdOpt, if (replierIsTestBedAdmin) communityOpt.map(_.fullname) else None, Some(targetOrg.id), Some(targetOrg.fullname)))
      }
    }
  }

  def resolveReplyTarget(parentMessageId: Long, replierOrgId: Long): Future[Option[ReplyTargetInfo]] = {
    DB.run(
      for {
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

  /** The full, alphabetically sorted recipient name list for a sent message - used to lazily expand the
   * "(N recipients)" indicator in the sent message detail panel. Scoped to the requesting organisation as
   * the sender: returns an empty list if the message wasn't sent by it. */
  def getMessageRecipientNames(messageId: Long, orgId: Long, viewerIsTestBedAdmin: Boolean): Future[List[String]] = {
    DB.run(PersistenceSchema.messages.filter(_.id === messageId).filter(_.senderId === orgId).exists.result).flatMap { owns =>
      if (owns) {
        DB.run(
          for {
            recipients <- PersistenceSchema.messageRecipients.filter(_.messageId === messageId).map(r => (r.recipientId, r.recipientNameSnapshot)).result
            adminNames <- resolveAdminPeerNames(recipients.flatMap(_._1).toSet, viewerIsTestBedAdmin)
          } yield recipients.map { case (recipientId, snapshot) => recipientId.flatMap(adminNames.get).getOrElse(snapshot) }.sorted.toList
        )
      } else {
        Future.successful(Nil)
      }
    }
  }

  def markReceivedMessagesRead(ids: List[Long], read: Boolean, orgId: Long): Future[Unit] = {
    val value: Option[Timestamp] = if (read) Some(TimeUtil.getCurrentTimestamp()) else None
    DB.run(
      PersistenceSchema.messageRecipients
        .filter(_.messageId inSet ids)
        .filter(_.recipientId === orgId)
        .map(_.readAt)
        .update(value)
        .transactionally
    ).map(_ => ())
  }

  def deleteReceivedMessages(ids: List[Long], orgId: Long): Future[Unit] = {
    DB.run(
      PersistenceSchema.messageRecipients
        .filter(_.messageId inSet ids)
        .filter(_.recipientId === orgId)
        .map(_.deletedByRecipientAt)
        .update(Some(TimeUtil.getCurrentTimestamp()))
        .transactionally
    ).map(_ => ())
  }

  def deleteSentMessages(ids: List[Long], orgId: Long): Future[Unit] = {
    DB.run(
      PersistenceSchema.messages
        .filter(_.id inSet ids)
        .filter(_.senderId === orgId)
        .map(_.deletedBySenderAt)
        .update(Some(TimeUtil.getCurrentTimestamp()))
        .transactionally
    ).map(_ => ())
  }

  /** Called when an organisation is deleted - nulls the (optional) FK columns referencing it, matching
   * the "deleting an organisation sets NULL on Messages.senderId / MessageRecipients.recipientId"
   * requirement. Must run before the organisation row itself is deleted. */
  private[managers] def clearOrganisationReferences(orgId: Long): DBIO[_] = {
    for {
      _ <- PersistenceSchema.messages.filter(_.senderId === orgId).map(_.senderId).update(None)
      _ <- PersistenceSchema.messageRecipients.filter(_.recipientId === orgId).map(_.recipientId).update(None)
    } yield ()
  }

  /** Called when one or more users are (hard-)deleted - nulls Messages.senderUserId for messages they
   * sent. Must run before the user rows themselves are deleted. */
  private[managers] def clearUserReferences(userIds: Seq[Long]): DBIO[_] = {
    if (userIds.isEmpty) {
      DBIO.successful(())
    } else {
      PersistenceSchema.messages.filter(_.senderUserId inSet userIds).map(_.senderUserId).update(None)
    }
  }

}
