package filters

import akka.stream.Materializer
import javax.inject.Inject
import play.api.mvc._
import play.api.http.HeaderNames.{ACCESS_CONTROL_ALLOW_ORIGIN, PRAGMA, _}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

object CorsFilter {
  def origin = "*"
  def methods = "GET, POST, PUT, DELETE"
  def headers = "Authorization, Origin, X-Requested-With, Content-Type, Accept, X-Custom-Header"
}

class CorsFilter @Inject() (implicit ec: ExecutionContext, implicit val mat: Materializer) extends EssentialFilter {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[CorsFilter])

  def apply(next: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      if (requestHeader.path.contains("font-awesome") || requestHeader.path.contains("fontawesome")) {
        next(requestHeader).map { result =>
          result.withHeaders(
            ACCESS_CONTROL_ALLOW_ORIGIN   -> CorsFilter.origin
          )
        }
      } else {
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
}