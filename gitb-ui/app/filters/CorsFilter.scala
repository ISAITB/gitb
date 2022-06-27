package filters

import akka.stream.Materializer
import org.slf4j.{Logger, LoggerFactory}
import play.api.http.HeaderNames._
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

object CorsFilter {
  def origin = "*"
  def methods = "GET, POST, PUT, DELETE, OPTIONS"
  def headers = "Authorization, Origin, X-Requested-With, Content-Type, Accept, X-Custom-Header, ITB_API_KEY"
}

class CorsFilter @Inject() (implicit ec: ExecutionContext, implicit val mat: Materializer) extends EssentialFilter {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[CorsFilter])

  def apply(next: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
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
            ACCESS_CONTROL_ALLOW_HEADERS -> CorsFilter.headers,
            PRAGMA -> "no-cache",
            EXPIRES -> "-1",
            CACHE_CONTROL -> "no-cache"
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