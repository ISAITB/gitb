import controllers.util.ResponseConstructor
import javax.inject.Singleton
import org.apache.commons.lang3.RandomStringUtils
import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

@Singleton
class ErrorHandler extends HttpErrorHandler {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val result = ResponseConstructor.constructServerError("Unexpected error", message, Some(""))
    Future.successful(result)
  }

  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] = {
    val errorIdentifier = RandomStringUtils.randomAlphabetic(10)
    Logger.error("Error ["+errorIdentifier+"]", ex)
    val result = ResponseConstructor.constructServerError("Unexpected error", ex.getMessage, Some(errorIdentifier))
    Future.successful(result)
  }
}
