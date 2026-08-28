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
import models.Enums.UserRole
import org.apache.commons.lang3.StringUtils
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MessageService @Inject()(authorizedAction: AuthorizedAction,
                               cc: ControllerComponents,
                               messageManager: MessageManager,
                               userManager: UserManager,
                               authorizationManager: AuthorizationManager)
                              (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private def ownOrganisationId(request: play.api.mvc.Request[AnyContent]): Future[Long] = {
    userManager.getById(ParameterExtractor.extractUserId(request)).map(_.organization)
  }

  /** The caller's own organisation id and whether they are a Test Bed administrator - used to render
   * admin-organisation peer names differently depending on the viewer, see MessageManager.resolveAdminPeerNames. */
  private def callerContext(request: play.api.mvc.Request[AnyContent]): Future[(Long, Boolean)] = {
    userManager.getById(ParameterExtractor.extractUserId(request)).map(u => (u.organization, u.role == UserRole.SystemAdmin.id.toShort))
  }

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
      callerContext(request).flatMap { case (orgId, viewerIsTestBedAdmin) =>
        messageManager.getReceivedMessages(orgId, page, limit, filterText, showRead, showUnread, showImportant, deliveredAfter, deliveredBefore, sortColumn, sortOrder, peerTargets, viewerIsTestBedAdmin).map { result =>
          val json: String = JsonUtil.jsSearchResult(result, JsonUtil.jsReceivedMessages).toString
          ResponseConstructor.constructJsonResponse(json)
        }
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
      callerContext(request).flatMap { case (orgId, viewerIsTestBedAdmin) =>
        messageManager.getSentMessages(orgId, page, limit, filterText, showImportant, createdAfter, createdBefore, sortColumn, sortOrder, peerTargets, viewerIsTestBedAdmin).map { result =>
          val json: String = JsonUtil.jsSearchResult(result, JsonUtil.jsSentMessages).toString
          ResponseConstructor.constructJsonResponse(json)
        }
      }
    }
  }

  /** Used only for the post-login unread-messages notification/menu badge - see MenuItemStatus and
   * IndexComponent.handlePostUserLoad on the frontend. */
  def hasUnreadMessages(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnMessages(request).flatMap { _ =>
      val userId = ParameterExtractor.extractUserId(request)
      userManager.getById(userId).flatMap { user =>
        messageManager.hasUnreadMessagesFromOthers(user.organization, userId).map { unread =>
          ResponseConstructor.constructJsonResponse(Json.obj("unread" -> unread).toString)
        }
      }
    }
  }

  def getMessage(messageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnMessages(request).flatMap { _ =>
      val sent = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.SENT).getOrElse(false)
      callerContext(request).flatMap { case (orgId, viewerIsTestBedAdmin) =>
        if (sent) {
          messageManager.getSentMessageDetail(messageId, orgId, viewerIsTestBedAdmin).map {
            case Some(detail) => ResponseConstructor.constructJsonResponse(JsonUtil.jsSentMessageDetail(detail).toString)
            case None => ResponseConstructor.constructNotFoundResponse(ErrorCodes.INVALID_PARAM, "The requested message could not be found.")
          }
        } else {
          messageManager.getReceivedMessageDetail(messageId, orgId, viewerIsTestBedAdmin).map {
            case Some(detail) => ResponseConstructor.constructJsonResponse(JsonUtil.jsReceivedMessageDetail(detail).toString)
            case None => ResponseConstructor.constructNotFoundResponse(ErrorCodes.INVALID_PARAM, "The requested message could not be found.")
          }
        }
      }
    }
  }

  def getMessageRecipients(messageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnMessages(request).flatMap { _ =>
      callerContext(request).flatMap { case (orgId, viewerIsTestBedAdmin) =>
        messageManager.getMessageRecipientNames(messageId, orgId, viewerIsTestBedAdmin).map { names =>
          ResponseConstructor.constructJsonResponse(JsonUtil.jsMessageRecipientNames(names).toString)
        }
      }
    }
  }

  def getMessageChain(messageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnMessages(request).flatMap { _ =>
      callerContext(request).flatMap { case (orgId, viewerIsTestBedAdmin) =>
        messageManager.getMessageChain(messageId, orgId, viewerIsTestBedAdmin).map { chain =>
          ResponseConstructor.constructJsonResponse(JsonUtil.jsMessageChain(chain).toString)
        }
      }
    }
  }

  def getReplyTarget(messageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnMessages(request).flatMap { _ =>
      callerContext(request).flatMap { case (orgId, _) =>
        messageManager.resolveReplyTarget(messageId, orgId).map {
          case Some(info) => ResponseConstructor.constructJsonResponse(JsonUtil.jsReplyTargetInfo(info).toString)
          case None => ResponseConstructor.constructJsonResponse(Json.obj().toString)
        }
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
    authorizationManager.canSendMessage(request, targets).flatMap { _ =>
      userManager.getById(userId).flatMap { user =>
        val messageIdFuture = parentMessageId match {
          // A reply: the recipient is now a normal, user-editable target list (authorised above exactly
          // like a new message) - the only reply-specific rule is that the sender must have been a party
          // (sender or recipient) to the message being replied to, enforced inside createMessageReply.
          case Some(pid) => messageManager.createMessageReply(user.organization, userId, pid, subject, body, important, targets)
          case None => messageManager.createMessage(user.organization, userId, subject, body, important, targets)
        }
        messageIdFuture.map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def updateMessageReadStatus(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageOwnMessages(request).flatMap { _ =>
      val ids = ParameterExtractor.extractLongIdsBodyParameter(request).getOrElse(List[Long]())
      val read = ParameterExtractor.requiredBodyParameter(request, ParameterNames.READ).toBoolean
      ownOrganisationId(request).flatMap { orgId =>
        messageManager.markReceivedMessagesRead(ids, read, orgId).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def deleteMessages(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageOwnMessages(request).flatMap { _ =>
      val ids = ParameterExtractor.extractLongIdsBodyParameter(request).getOrElse(List[Long]())
      val sent = ParameterExtractor.optionalBooleanBodyParameter(request, ParameterNames.SENT).getOrElse(false)
      ownOrganisationId(request).flatMap { orgId =>
        val result = if (sent) messageManager.deleteSentMessages(ids, orgId) else messageManager.deleteReceivedMessages(ids, orgId)
        result.map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

}
