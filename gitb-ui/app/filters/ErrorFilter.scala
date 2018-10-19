package filters

import java.util.concurrent.TimeoutException

import com.gitb.tbs.Error
import controllers.util.ResponseConstructor
import exceptions._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

class ErrorFilter extends Filter{

  def apply(next: (RequestHeader) => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    next(requestHeader) recover  {

      case e:Error =>
        ResponseConstructor.constructServerError(e.getFaultInfo.getErrorCode.value(), e.getFaultInfo.getDescription, None)

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
