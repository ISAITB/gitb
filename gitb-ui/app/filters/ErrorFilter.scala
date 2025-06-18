/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package filters

import java.util.concurrent.TimeoutException

import org.apache.pekko.stream.Materializer
import com.gitb.tbs.Error
import controllers.util.ResponseConstructor
import exceptions._
import javax.inject.Inject
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class ErrorFilter @Inject() (implicit ec: ExecutionContext, implicit val mat: Materializer) extends Filter {

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

      case e:UnauthorizedAccessException =>
        ResponseConstructor.constructAccessDeniedResponse(403, e.msg)

    }
  }
}
