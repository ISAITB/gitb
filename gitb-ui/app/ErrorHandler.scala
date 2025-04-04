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
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject() (legalNoticeManager: LegalNoticeManager)
                             (implicit ec: ExecutionContext) extends HttpErrorHandler {

  private def logger = LoggerFactory.getLogger(this.getClass)

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful {
      if (statusCode == 404) {
        NotFound
      } else {
        ResponseConstructor.constructServerError("Unexpected error", message, Some(""))
      }
    }
  }

  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] = {
    val requestedWithHeader = request.headers.get("X-Requested-With")
    val acceptHeader = request.headers.get("Accept")
    val errorAsJson = requestedWithHeader.isDefined || acceptHeader.exists(x => StringUtils.contains(x.toLowerCase, "application/json")) || request.path.startsWith(Configurations.API_ROOT)
    val errorIdentifier = RandomStringUtils.secure().nextAlphabetic(10).toUpperCase()
    logger.error("Error ["+errorIdentifier+"]", ex)
    if (errorAsJson) {
      val result = ResponseConstructor.constructServerError("Unexpected error", "An unexpected error occurred.", Some(errorIdentifier))
      Future.successful(result)
    } else {
      legalNoticeManager.getCommunityDefaultLegalNotice(Constants.DefaultCommunityId).map { legalNotice =>
        if (legalNotice.isDefined && !StringUtils.isBlank(legalNotice.get.content)) {
          Some(legalNotice.get.content)
        } else {
          None
        }
      }.map { legalNoticeContent =>
        val versionInfo = Configurations.versionInfo()
        BadRequest(views.html.error(
          new ErrorPageData(
            versionInfo,
            versionInfo.replace(' ', '_'),
            legalNoticeContent.isDefined,
            legalNoticeContent.getOrElse(""),
            Configurations.USERGUIDE_OU,
            errorIdentifier,
            Configurations.TESTBED_HOME_LINK,
            Configurations.RELEASE_INFO_ENABLED,
            Configurations.RELEASE_INFO_ADDRESS,
            Configurations.PUBLIC_CONTEXT_ROOT_WITH_SLASH,
            Configurations.restApiLink()
          ))
        )
      }.recover {
        case e: Exception =>
          logger.error("Error while creating error page content", e)
          BadRequest
      }
    }
  }

}
