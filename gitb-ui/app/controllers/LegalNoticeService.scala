package controllers

import controllers.util.{Parameters, ParameterExtractor, ResponseConstructor}
import exceptions.ErrorCodes
import managers.LegalNoticeManager
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc.{Action, Controller}
import utils.JsonUtil
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class LegalNoticeService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[LegalNoticeService])

  /**
   * Gets all legal notices for the specified community
   */
  def getLegalNoticesByCommunity(communityId: Long) = Action.async {
    LegalNoticeManager.getLegalNoticesByCommunity(communityId) map { list =>
      val json: String = JsonUtil.jsLegalNotices(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Creates new legal notice
   */
  def createLegalNotice() = Action.async { request =>
    val legalNotice = ParameterExtractor.extractLegalNoticeInfo(request)
    LegalNoticeManager.checkUniqueName(legalNotice.name, legalNotice.community) map { uniqueName =>
      if (uniqueName) {
        LegalNoticeManager.createLegalNotice(legalNotice)
        ResponseConstructor.constructEmptyResponse
      } else {
        ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Legal notice with '" + legalNotice.name + "' already exists.")
      }
    }
  }

  /**
   * Gets the legal notice with specified id
   */
  def getLegalNoticeById(noticeId: Long) = Action.async { request =>
    LegalNoticeManager.getLegalNoticeById(noticeId) map { ln =>
      val json: String = JsonUtil.serializeLegalNotice(ln)
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Updates legal notice
   */
  def updateLegalNotice(noticeId: Long) = Action.async { request =>
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val description = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
    val content = ParameterExtractor.requiredBodyParameter(request, Parameters.CONTENT)
    val default = ParameterExtractor.requiredBodyParameter(request, Parameters.DEFAULT).toBoolean
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong

    LegalNoticeManager.checkUniqueName(noticeId, name, communityId) map { uniqueName =>
      if (uniqueName) {
        LegalNoticeManager.updateLegalNotice(noticeId, name, description, content, default, communityId)
        ResponseConstructor.constructEmptyResponse
      } else {
        ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Legal notice with '" + name + "' already exists.")
      }
    }
  }

  /**
   * Deletes legal notice with specified id
   */
  def deleteLegalNotice(noticeId: Long) = Action.async { request =>
    LegalNoticeManager.deleteLegalNotice(noticeId) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
    * Gets the default landing page for given community
    */
  def getCommunityDefaultLegalNotice() = Action.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    LegalNoticeManager.getCommunityDefaultLegalNotice(communityId) map { legalNotice =>
      val json: String = JsonUtil.serializeLegalNotice(legalNotice)
      ResponseConstructor.constructJsonResponse(json)
    }
  }


}