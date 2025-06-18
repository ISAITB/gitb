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

import org.apache.pekko.stream.Materializer
import javax.inject.Inject
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

class HeaderFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  def apply(next: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    next(requestHeader).map { result =>
      result.withHeaders(
        "X-Content-Type-Options" -> "nosniff",
        "Referrer-Policy" -> "same-origin",
        "X-Frame-Options" -> "deny",
        "Permissions-Policy" -> "accelerometer=(), autoplay=(), camera=(), cross-origin-isolated=(), display-capture=(), encrypted-media=(), fullscreen=(), geolocation=(), gyroscope=(), keyboard-map=(), magnetometer=(), microphone=(), midi=(), payment=(), picture-in-picture=(), publickey-credentials-get=(), screen-wake-lock=(), sync-xhr=(self), usb=(), web-share=(), xr-spatial-tracking=(), clipboard-read=(self), clipboard-write=(self), gamepad=()",
        "Content-Security-Policy" -> "default-src 'none'; font-src 'self'; script-src 'self'; connect-src 'self'; style-src 'unsafe-inline' 'self'; img-src * data:; base-uri 'self'; frame-ancestors 'none';"
      )
    }
  }

}
