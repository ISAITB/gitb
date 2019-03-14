package controllers

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.LegalNoticeManager
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{Action, Controller}
import utils.JsonUtil

class LegalNoticeService @Inject() (legalNoticeManager: LegalNoticeManager) extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[LegalNoticeService])

  /**
   * Gets all legal notices for the specified community
   */
  def getLegalNoticesByCommunity(communityId: Long) = Action.apply {
    val list = legalNoticeManager.getLegalNoticesByCommunity(communityId)
    val json: String = JsonUtil.jsLegalNotices(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new legal notice
   */
  def createLegalNotice() = Action.apply { request =>
    val legalNotice = ParameterExtractor.extractLegalNoticeInfo(request)
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
  def getLegalNoticeById(noticeId: Long) = Action.apply { request =>
    val ln = legalNoticeManager.getLegalNoticeById(noticeId)
    val json: String = JsonUtil.serializeLegalNotice(ln)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Updates legal notice
   */
  def updateLegalNotice(noticeId: Long) = Action.apply { request =>
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val description = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
    val content = ParameterExtractor.requiredBodyParameter(request, Parameters.CONTENT)
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
  def deleteLegalNotice(noticeId: Long) = Action.apply { request =>
    legalNoticeManager.deleteLegalNotice(noticeId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Gets the default landing page for given community
    */
  def getCommunityDefaultLegalNotice() = Action.apply { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    val legalNotice = legalNoticeManager.getCommunityDefaultLegalNotice(communityId)
    val json: String = JsonUtil.serializeLegalNotice(legalNotice)
    ResponseConstructor.constructJsonResponse(json)
  }


}