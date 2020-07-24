import config.Configurations
import controllers.util.ResponseConstructor
import javax.inject.{Inject, Singleton}
import managers.{LegalNoticeManager, SystemConfigurationManager}
import models.{Constants, ErrorPageData}
import org.apache.commons.lang3.{RandomStringUtils, StringUtils}
import org.slf4j.LoggerFactory
import org.webjars.play.WebJarsUtil
import play.api.http.HttpErrorHandler
import play.api.mvc.Results.BadRequest
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject() (systemConfigurationManager: SystemConfigurationManager, legalNoticeManager: LegalNoticeManager) extends HttpErrorHandler {

  private def logger = LoggerFactory.getLogger(this.getClass)

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val result = ResponseConstructor.constructServerError("Unexpected error", message, Some(""))
    Future.successful(result)
  }

  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] = {
    val requestedWithHeader = request.headers.get("X-Requested-With")
    val acceptHeader = request.headers.get("Accept")
    val errorAsJson = requestedWithHeader.isDefined || (acceptHeader.isDefined && StringUtils.contains(acceptHeader.get.toLowerCase, "application/json"))
    val errorIdentifier = RandomStringUtils.randomAlphabetic(10)
    logger.error("Error ["+errorIdentifier+"]", ex)
    if (errorAsJson) {
      val result = ResponseConstructor.constructServerError("Unexpected error", ex.getMessage, Some(errorIdentifier))
      Future.successful(result)
    } else {
      var legalNoticeContent = ""
      try {
        val legalNotice = legalNoticeManager.getCommunityDefaultLegalNotice(Constants.DefaultCommunityId)
        if (legalNotice.isDefined) {
          legalNoticeContent = legalNotice.get.content
        }
      } catch {
        case e:Exception => {
          logger.error("Error while creating error page content", e)
        }
      }
      Future.successful(BadRequest(views.html.error(
        new ErrorPageData(
          systemConfigurationManager.getLogoPath(),
          systemConfigurationManager.getFooterLogoPath(),
          Constants.VersionNumber,
          legalNoticeContent,
          Configurations.USERGUIDE_OU,
          errorIdentifier,
          Configurations.TESTBED_HOME_LINK
        )))
      )
    }
  }
}
