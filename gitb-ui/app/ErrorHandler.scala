import config.Configurations
import controllers.util.ResponseConstructor
import managers.LegalNoticeManager
import models.{Constants, ErrorPageData}
import org.apache.commons.lang3.{RandomStringUtils, StringUtils}
import org.slf4j.LoggerFactory
import play.api.http.HttpErrorHandler
import play.api.mvc.Results.{BadRequest, NotFound}
import play.api.mvc.{RequestHeader, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject() (legalNoticeManager: LegalNoticeManager) extends HttpErrorHandler {

  private def logger = LoggerFactory.getLogger(this.getClass)

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    if (statusCode == 404) {
      Future.successful(NotFound)
    } else {
      Future.successful(ResponseConstructor.constructServerError("Unexpected error", message, Some("")))
    }
  }

  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] = {
    val requestedWithHeader = request.headers.get("X-Requested-With")
    val acceptHeader = request.headers.get("Accept")
    val errorAsJson = requestedWithHeader.isDefined || (acceptHeader.isDefined && StringUtils.contains(acceptHeader.get.toLowerCase, "application/json"))
    val errorIdentifier = RandomStringUtils.secure().nextAlphabetic(10).toUpperCase()
    logger.error("Error ["+errorIdentifier+"]", ex)
    if (errorAsJson) {
      val result = ResponseConstructor.constructServerError("Unexpected error", "An unexpected error occurred.", Some(errorIdentifier))
      Future.successful(result)
    } else {
      var legalNoticeContent = ""
      var hasDefaultLegalNotice = false
      try {
        val legalNotice = legalNoticeManager.getCommunityDefaultLegalNotice(Constants.DefaultCommunityId)
        if (legalNotice.isDefined && !StringUtils.isBlank(legalNotice.get.content)) {
          hasDefaultLegalNotice = true
          legalNoticeContent = legalNotice.get.content
        }
      } catch {
        case e:Exception =>
          logger.error("Error while creating error page content", e)
      }
      val versionInfo = Configurations.versionInfo()
      Future.successful(BadRequest(views.html.error(
        new ErrorPageData(
          versionInfo,
          versionInfo.replace(' ', '_'),
          hasDefaultLegalNotice,
          legalNoticeContent,
          Configurations.USERGUIDE_OU,
          errorIdentifier,
          Configurations.TESTBED_HOME_LINK,
          Configurations.RELEASE_INFO_ENABLED,
          Configurations.RELEASE_INFO_ADDRESS,
          Configurations.PUBLIC_CONTEXT_ROOT_WITH_SLASH
        )))
      )
    }
  }

}
