package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import managers.{AuthorizationManager, LegalNoticeManager}
import models.Constants
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.{HtmlUtil, JsonUtil}

import javax.inject.Inject

class LegalNoticeService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, legalNoticeManager: LegalNoticeManager, authorizationManager: AuthorizationManager) extends AbstractController(cc) {

  /**
   * Gets all legal notices for the specified community
   */
  def getLegalNoticesByCommunity(communityId: Long) = authorizedAction { request =>
    authorizationManager.canManageLegalNotices(request, communityId)
    val list = legalNoticeManager.getLegalNoticesByCommunityWithoutContent(communityId)
    val json: String = JsonUtil.jsLegalNotices(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new legal notice
   */
  def createLegalNotice() = authorizedAction { request =>
    val legalNotice = ParameterExtractor.extractLegalNoticeInfo(request)
    authorizationManager.canManageLegalNotices(request, legalNotice.community)
    val uniqueName = legalNoticeManager.checkUniqueName(legalNotice.name, legalNotice.community)
    if (uniqueName) {
      legalNoticeManager.createLegalNotice(legalNotice)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Legal notice with name '" + legalNotice.name + "' already exists.")
    }
  }

  /**
   * Gets the legal notice with specified id
   */
  def getLegalNoticeById(noticeId: Long) = authorizedAction { request =>
    authorizationManager.canManageLegalNotice(request, noticeId)
    val ln = legalNoticeManager.getLegalNoticeById(noticeId)
    val json: String = JsonUtil.serializeLegalNotice(ln)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Updates legal notice
   */
  def updateLegalNotice(noticeId: Long) = authorizedAction { request =>
    authorizationManager.canManageLegalNotice(request, noticeId)
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val description = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
    val content = HtmlUtil.sanitizeEditorContent(ParameterExtractor.requiredBodyParameter(request, Parameters.CONTENT))
    val default = ParameterExtractor.requiredBodyParameter(request, Parameters.DEFAULT).toBoolean
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong

    val uniqueName = legalNoticeManager.checkUniqueName(noticeId, name, communityId)
    if (uniqueName) {
      legalNoticeManager.updateLegalNotice(noticeId, name, description, content, default, communityId)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Legal notice with name '" + name + "' already exists.")
    }
  }

  /**
   * Deletes legal notice with specified id
   */
  def deleteLegalNotice(noticeId: Long) = authorizedAction { request =>
    authorizationManager.canManageLegalNotice(request, noticeId)
    legalNoticeManager.deleteLegalNotice(noticeId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Gets the default landing page for given community
    */
  def getCommunityDefaultLegalNotice() = authorizedAction { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewDefaultLegalNotice(request, communityId)
    var legalNotice = legalNoticeManager.getCommunityDefaultLegalNotice(communityId)
    if (legalNotice.isEmpty) {
      legalNotice = legalNoticeManager.getCommunityDefaultLegalNotice(Constants.DefaultCommunityId)
    }
    val json: String = JsonUtil.serializeLegalNotice(legalNotice)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
    * Gets the default landing page for the test bed.
    */
  def getTestBedDefaultLegalNotice() = authorizedAction { request =>
    authorizationManager.canViewTestBedDefaultLegalNotice(request)
    val legalNotice = legalNoticeManager.getCommunityDefaultLegalNotice(Constants.DefaultCommunityId)
    val json: String = JsonUtil.serializeLegalNotice(legalNotice)
    ResponseConstructor.constructJsonResponse(json)
  }

}