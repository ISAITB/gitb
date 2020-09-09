package filters

import akka.stream.Materializer
import javax.inject.Inject
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

class HeaderFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  def apply(next: (RequestHeader) => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    next(requestHeader).map { result =>
      result.withHeaders(
        "X-Content-Type-Options" -> "nosniff",
        "Referrer-Policy" -> "same-origin",
        "X-Frame-Options" -> "deny",
        "X-Xss-Protection" -> "X-XSS-Protection: 1; mode=block",
        "Feature-Policy" -> "accelerometer 'none'; autoplay 'none'; camera 'none'; document-domain 'none'; encrypted-media 'none'; fullscreen 'none'; geolocation 'none'; gyroscope 'none'; magnetometer 'none'; microphone 'none'; midi 'none'; payment 'none'; picture-in-picture 'none'; sync-xhr 'self'; xr-spatial-tracking 'none';",
        "Content-Security-Policy" -> "default-src 'none'; font-src 'self'; script-src 'self' 'unsafe-inline'; connect-src 'self'; style-src 'unsafe-inline' 'self'; img-src * data:; base-uri 'self'; frame-ancestors 'none';"
      )
    }
  }

}
