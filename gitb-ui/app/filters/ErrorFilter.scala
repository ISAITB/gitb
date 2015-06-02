package filters

import com.gitb.tbs.Error
import play.api.mvc._
import exceptions._
import scala.concurrent.Future
import controllers.util.ResponseConstructor
import java.util.concurrent.TimeoutException
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{LoggerFactory, Logger}

class ErrorFilter extends Filter{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[ErrorFilter])

  def apply(next: (RequestHeader) => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    next(requestHeader) recover  {

      case e:Error =>
        ResponseConstructor.constructServerError(e.getFaultInfo.getErrorCode.value(), e.getFaultInfo.getDescription)

      case e:InvalidRequestException =>
        ResponseConstructor.constructBadRequestResponse(e.error, e.msg)

      case e:InvalidAuthorizationException =>
        ResponseConstructor.constructUnauthorizedResponse(e.error, e.getMessage)

      case e:InvalidTokenException =>
        ResponseConstructor.constructUnauthorizedResponse(e.error, e.msg)

      case e:NotFoundException =>
        ResponseConstructor.constructNotFoundResponse(e.error, e.msg)

      case e:TimeoutException =>
        ResponseConstructor.constructTimeoutResponse
    }
  }
}
