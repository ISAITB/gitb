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

import config.Configurations
import org.apache.pekko.stream.Materializer
import play.api.http.HeaderNames._
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

object CorsFilter {
  def origin = "*"
  def methods = "GET, POST, PUT, DELETE, OPTIONS"
}

class CorsFilter @Inject() (implicit ec: ExecutionContext, implicit val mat: Materializer) extends EssentialFilter {

  def apply(next: EssentialAction): EssentialAction = (requestHeader: RequestHeader) => {
    if (requestHeader.path.contains("font-awesome") || requestHeader.path.contains("fontawesome")) {
      next(requestHeader).map { result =>
        result.withHeaders(
          ACCESS_CONTROL_ALLOW_ORIGIN -> CorsFilter.origin
        )
      }
    } else if (requestHeader.path.contains("/api/rest")) {
      next(requestHeader).map { result =>
        result.withHeaders(
          ALLOW -> CorsFilter.origin,
          ACCESS_CONTROL_ALLOW_ORIGIN -> CorsFilter.origin,
          ACCESS_CONTROL_ALLOW_METHODS -> CorsFilter.methods,
          ACCESS_CONTROL_ALLOW_HEADERS -> Configurations.HEADERS_FOR_CORS_FILTERING,
          PRAGMA -> "no-cache",
          EXPIRES -> "-1",
          CACHE_CONTROL -> "no-cache"
        )
      }
    } else {
      next(requestHeader).map { result =>
        result.withHeaders(
          ACCESS_CONTROL_ALLOW_ORIGIN -> CorsFilter.origin,
          PRAGMA -> "no-cache",
          EXPIRES -> "-1",
          CACHE_CONTROL -> "no-cache"
        )
      }
    }
  }
}