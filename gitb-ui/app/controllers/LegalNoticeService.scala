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
   * Gets all legal notices
   */
  def getLegalNotices() = Action.async {
    LegalNoticeManager.getLegalNotices() map { list =>
      val json: String = JsonUtil.jsLegalNotices(list).toString
      ResponseConstructor.constructJsonResponse(json)
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
   * Gets the default legal notice
   */
  def getDefaultLegalNotice() = Action.async { request =>
    LegalNoticeManager.getDefaultLegalNotice() map { ln =>
      if (ln != null) {
        val json: String = JsonUtil.serializeLegalNotice(ln)
        ResponseConstructor.constructJsonResponse(json)
      } else {
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
   * Creates new legal notice
   */
  def createLegalNotice() = Action.async { request =>
    val ln = ParameterExtractor.extractLegalNoticeInfo(request)
    LegalNoticeManager.checkUniqueName(ln.name) map { uniqueName =>
      if (uniqueName) {
        LegalNoticeManager.createLegalNotice(ln)
        ResponseConstructor.constructEmptyResponse
      } else {
        ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Legal notice with '" + ln.name + "' already exists.")
      }
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

    LegalNoticeManager.checkUniqueName(name, noticeId) map { uniqueName =>
      if (uniqueName) {
        LegalNoticeManager.updateLegalNotice(noticeId, name, description, content, default)
        ResponseConstructor.constructEmptyResponse
      } else {
        ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Landing page with '" + name + "' already exists.")
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

}
