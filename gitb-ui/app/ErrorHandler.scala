import config.Configurations
import controllers.util.ResponseConstructor
import controllers.{SystemConfigurationService, WebJarAssets}
import javax.inject.{Inject, Singleton}
import managers.LegalNoticeManager
import models.{Constants, ErrorPageData}
import org.apache.commons.lang3.{RandomStringUtils, StringUtils}
import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.mvc.Results.BadRequest
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject() (systemConfigurationService: SystemConfigurationService, legalNoticeManager: LegalNoticeManager) (implicit webJarAssets: WebJarAssets) extends HttpErrorHandler {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val result = ResponseConstructor.constructServerError("Unexpected error", message, Some(""))
    Future.successful(result)
  }

  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] = {
    val requestedWithHeader = request.headers.get("X-Requested-With")
    val acceptHeader = request.headers.get("Accept")
    val errorAsJson = requestedWithHeader.isDefined || (acceptHeader.isDefined && StringUtils.contains(acceptHeader.get.toLowerCase, "application/json"))
    val errorIdentifier = RandomStringUtils.randomAlphabetic(10)
    Logger.error("Error ["+errorIdentifier+"]", ex)
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
          Logger.error("Error while creating error page content", e)
        }
      }
      Future.successful(BadRequest(views.html.error(webJarAssets,
        new ErrorPageData(
          systemConfigurationService.getLogoPath(),
          systemConfigurationService.getFooterLogoPath(),
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
