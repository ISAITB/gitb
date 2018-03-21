package filters

import play.api.mvc._
import play.api.http.HeaderNames.{ACCESS_CONTROL_ALLOW_ORIGIN, PRAGMA, _}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{Logger, LoggerFactory}

object CorsFilter {
  def origin = "*"
  def methods = "GET, POST, PUT, DELETE"
  def headers = "Authorization, Origin, X-Requested-With, Content-Type, Accept, X-Custom-Header"
}

class CorsFilter extends EssentialFilter {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[CorsFilter])

  def apply(next: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      next(requestHeader).map { result =>
        result.withHeaders(
          ACCESS_CONTROL_ALLOW_ORIGIN   -> CorsFilter.origin,
          PRAGMA -> "no-cache",
          EXPIRES -> "-1",
          CACHE_CONTROL -> "no-cache"
        )
      }
    }
  }
}