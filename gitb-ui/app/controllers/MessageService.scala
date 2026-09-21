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

package controllers

import controllers.util._
import exceptions.ErrorCodes
import managers.{AuthorizationManager, MessageManager, UserManager}
import org.apache.commons.lang3.StringUtils
import play.api.libs.json.Json
import play.api.mvc._
import utils.JsonUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MessageService @Inject()(authorizedAction: AuthorizedAction,
                               cc: ControllerComponents,
                               messageManager: MessageManager,
                               userManager: UserManager,
                               authorizationManager: AuthorizationManager)
                              (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getReceivedMessages(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnMessages(request).flatMap { _ =>
      val page = ParameterExtractor.extractPageNumber(request)
      val limit = ParameterExtractor.extractPageLimit(request)
      val filterText = ParameterExtractor.optionalQueryParameter(request, ParameterNames.FILTER).filter(StringUtils.isNotBlank)
      val showRead = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.SHOW_READ).getOrElse(true)
      val showUnread = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.SHOW_UNREAD).getOrElse(true)
      val showImportant = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.SHOW_IMPORTANT).getOrElse(false)
      val deliveredAfter = ParameterExtractor.optionalQueryParameter(request, ParameterNames.START_TIME_BEGIN)
      val deliveredBefore = ParameterExtractor.optionalQueryParameter(request, ParameterNames.START_TIME_END)
      val sortColumn = ParameterExtractor.optionalQueryParameter(request, ParameterNames.SORT_COLUMN)
      val sortOrder = ParameterExtractor.optionalQueryParameter(request, ParameterNames.SORT_ORDER)
      val peerTargets = ParameterExtractor.optionalQueryParameter(request, ParameterNames.PEER_TARGETS).map(JsonUtil.parseJsMessageTargets).getOrElse(List())
      val userId = ParameterExtractor.extractUserId(request)
      messageManager.getReceivedMessages(userId, page, limit, filterText, showRead, showUnread, showImportant, deliveredAfter, deliveredBefore, sortColumn, sortOrder, peerTargets).map { result =>
        val json: String = JsonUtil.jsSearchResult(result, JsonUtil.jsReceivedMessages).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getSentMessages(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnMessages(request).flatMap { _ =>
      val page = ParameterExtractor.extractPageNumber(request)
      val limit = ParameterExtractor.extractPageLimit(request)
      val filterText = ParameterExtractor.optionalQueryParameter(request, ParameterNames.FILTER).filter(StringUtils.isNotBlank)
      val showImportant = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.SHOW_IMPORTANT).getOrElse(false)
      val createdAfter = ParameterExtractor.optionalQueryParameter(request, ParameterNames.START_TIME_BEGIN)
      val createdBefore = ParameterExtractor.optionalQueryParameter(request, ParameterNames.START_TIME_END)
      val sortColumn = ParameterExtractor.optionalQueryParameter(request, ParameterNames.SORT_COLUMN)
      val sortOrder = ParameterExtractor.optionalQueryParameter(request, ParameterNames.SORT_ORDER)
      val peerTargets = ParameterExtractor.optionalQueryParameter(request, ParameterNames.PEER_TARGETS).map(JsonUtil.parseJsMessageTargets).getOrElse(List())
      val userId = ParameterExtractor.extractUserId(request)
      messageManager.getSentMessages(userId, page, limit, filterText, showImportant, createdAfter, createdBefore, sortColumn, sortOrder, peerTargets).map { result =>
        val json: String = JsonUtil.jsSearchResult(result, JsonUtil.jsSentMessages).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Used only for the post-login unread-messages notification/menu badge.
   */
  def hasUnreadMessages(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnMessages(request).flatMap { _ =>
      val userId = ParameterExtractor.extractUserId(request)
      messageManager.hasUnreadMessages(userId).map { unread =>
        ResponseConstructor.constructJsonResponse(Json.obj("unread" -> unread).toString)
      }
    }
  }

  def getMessageWithChainAsCommunityAdmin(messageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val sent = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.SENT).getOrElse(false)
    authorizationManager.canViewMessage(request, messageId, requireTestBedAdmin = false, requireCommunityAdmin = true, sentMessage = Some(sent)).flatMap { case (_, orgId, userId) =>
      getMessageWithChainInternal(messageId, sent, orgId, userId, isCommunityAdmin = true, isTestBedAdmin = false)
    }
  }

  def getMessageWithChainAsTestBedAdmin(messageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val sent = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.SENT).getOrElse(false)
    authorizationManager.canViewMessage(request, messageId, requireTestBedAdmin = true, requireCommunityAdmin = false, sentMessage = Some(sent)).flatMap { case (_, orgId, userId) =>
      getMessageWithChainInternal(messageId, sent, orgId, userId, isCommunityAdmin = false, isTestBedAdmin = true)
    }
  }

  def getMessageWithChain(messageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val sent = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.SENT).getOrElse(false)
    authorizationManager.canViewMessage(request, messageId, requireTestBedAdmin = false, requireCommunityAdmin = false, sentMessage = Some(sent)).flatMap { case (_, orgId, userId) =>
      getMessageWithChainInternal(messageId, sent, orgId, userId, isCommunityAdmin = false, isTestBedAdmin = false)
    }
  }

  private def getMessageWithChainInternal(messageId: Long, sentMessage: Boolean, userOrganisation: Long, userId: Long, isCommunityAdmin: Boolean, isTestBedAdmin: Boolean): Future[Result] = {
    if (sentMessage) {
      messageManager.getSentMessageWithChain(messageId, userId, userOrganisation, isCommunityAdmin, isTestBedAdmin).map {
        case Some((detail, chain)) => ResponseConstructor.constructJsonResponse(JsonUtil.jsSentMessageWithChain(detail, chain).toString)
        case None => ResponseConstructor.constructNotFoundResponse(ErrorCodes.INVALID_PARAM, "The requested message could not be found.")
      }
    } else {
      messageManager.getReceivedMessageWithChain(messageId, userId, userOrganisation, isCommunityAdmin, isTestBedAdmin).map {
        case Some((detail, chain)) => ResponseConstructor.constructJsonResponse(JsonUtil.jsReceivedMessageWithChain(detail, chain).toString)
        case None => ResponseConstructor.constructNotFoundResponse(ErrorCodes.INVALID_PARAM, "The requested message could not be found.")
      }
    }
  }

  def getMessageRecipients(messageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnMessages(request).flatMap { _ =>
      val userId = ParameterExtractor.extractUserId(request)
      messageManager.getMessageRecipientNames(userId, messageId).map { names =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsMessageRecipientNames(names).toString)
      }
    }
  }

  def getMessageChainAsCommunityAdmin(messageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewMessage(request, messageId, requireTestBedAdmin = false, requireCommunityAdmin = true, sentMessage = None).flatMap { case (_, orgId, userId) =>
      getMessageChainInternal(messageId, orgId, userId, isCommunityAdmin = true, isTestBedAdmin = false)
    }
  }

  def getMessageChainAsTestBedAdmin(messageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewMessage(request, messageId, requireTestBedAdmin = true, requireCommunityAdmin = false, sentMessage = None).flatMap { case (_, orgId, userId) =>
      getMessageChainInternal(messageId, orgId, userId, isCommunityAdmin = false, isTestBedAdmin = true)
    }
  }

  def getMessageChain(messageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewMessage(request, messageId, requireTestBedAdmin = false, requireCommunityAdmin = false, sentMessage = None).flatMap { case (_, orgId, userId) =>
      getMessageChainInternal(messageId, orgId, userId, isCommunityAdmin = false, isTestBedAdmin = false)
    }
  }

  private def getMessageChainInternal(messageId: Long, userOrganisation: Long, userId: Long, isCommunityAdmin: Boolean, isTestBedAdmin: Boolean): Future[Result] = {
    messageManager.getMessageChain(messageId, userOrganisation, userId, isTestBedAdmin, isCommunityAdmin).map { chain =>
      ResponseConstructor.constructJsonResponse(JsonUtil.jsMessageChain(chain).toString)
    }
  }

  def getReplyTarget(messageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnMessages(request).flatMap { _ =>
      val userId = ParameterExtractor.extractUserId(request)
      messageManager.resolveReplyTarget(userId, messageId).map {
        case Some(info) => ResponseConstructor.constructJsonResponse(JsonUtil.jsReplyTargetInfo(info).toString)
        case None => ResponseConstructor.constructJsonResponse(Json.obj().toString)
      }
    }
  }

  def createMessage(): Action[AnyContent] = authorizedAction.async { request =>
    val subject = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SUBJECT).filter(StringUtils.isNotBlank)
    val body = ParameterExtractor.optionalBodyParameter(request, ParameterNames.BODY).filter(StringUtils.isNotBlank)
    val important = ParameterExtractor.optionalBooleanBodyParameter(request, ParameterNames.IMPORTANT).getOrElse(false)
    val userId = ParameterExtractor.extractUserId(request)
    val parentMessageId = ParameterExtractor.optionalLongBodyParameter(request, ParameterNames.PARENT_MESSAGE_ID)
    val targets = JsonUtil.parseJsMessageTargets(ParameterExtractor.requiredBodyParameter(request, ParameterNames.RECIPIENTS))
    authorizationManager.canSendMessage(request, targets, parentMessageId).flatMap { _ =>
      val messageIdFuture = parentMessageId match {
        case Some(pid) => messageManager.createMessageReply(userId, pid, subject, body, important, targets)
        case None => messageManager.createMessage(userId, subject, body, important, targets)
      }
      messageIdFuture.map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def updateMessageReadStatus(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageOwnMessages(request).flatMap { _ =>
      val ids = ParameterExtractor.extractLongIdsBodyParameter(request).getOrElse(List[Long]())
      val read = ParameterExtractor.requiredBodyParameter(request, ParameterNames.READ).toBoolean
      val userId = ParameterExtractor.extractUserId(request)
      messageManager.markReceivedMessagesRead(ids, read, userId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def deleteMessages(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageOwnMessages(request).flatMap { _ =>
      val ids = ParameterExtractor.extractLongIdsBodyParameter(request).getOrElse(List[Long]())
      val sent = ParameterExtractor.optionalBooleanBodyParameter(request, ParameterNames.SENT).getOrElse(false)
      val userId = ParameterExtractor.extractUserId(request)
      val result = if (sent) messageManager.deleteSentMessages(ids, userId) else messageManager.deleteReceivedMessages(ids, userId)
      result.map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

}
