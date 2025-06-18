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

package controllers.util

import javax.inject.Inject
import managers.AuthorizationManager
import play.api.mvc.{Request, _}

import scala.concurrent.{ExecutionContext, Future}

class AuthorizedAction @Inject() (val parser: BodyParsers.Default) (implicit val ec: ExecutionContext) extends ActionBuilder[RequestWithAttributes, AnyContent] {

  def wrap[A](request: Request[A], block: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    // Perform an input sanitization check first. This is done here and not via filter to benefit from the already parsed request body.
    InputSanitizer.check(request)
    val enhancedRequest = RequestWithAttributes(request)
    block(enhancedRequest).map(result =>
      if (enhancedRequest.authorised) {
        result
      } else {
        throw new IllegalStateException("Authorization check missing for path ["+enhancedRequest.path+"]")
      }
    )
  }

  def invokeBlock[A](request: Request[A], block: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    wrap(request, block)
  }

  override protected def executionContext: ExecutionContext = ec

}

