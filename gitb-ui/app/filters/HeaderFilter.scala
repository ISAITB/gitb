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
        "Permissions-Policy" -> "accelerometer=(), ambient-light-sensor=(), autoplay=(), battery=(), camera=(), cross-origin-isolated=(), display-capture=(), document-domain=(), encrypted-media=(), execution-while-not-rendered=(), execution-while-out-of-viewport=(), fullscreen=(), geolocation=(), gyroscope=(), keyboard-map=(), magnetometer=(), microphone=(), midi=(), navigation-override=(), payment=(), picture-in-picture=(), publickey-credentials-get=(), screen-wake-lock=(), sync-xhr=(self), usb=(), web-share=(), xr-spatial-tracking=(), clipboard-read=(self), clipboard-write=(self), gamepad=(), speaker-selection=()",
        "Content-Security-Policy" -> "default-src 'none'; font-src 'self'; script-src 'self'; connect-src 'self'; style-src 'unsafe-inline' 'self'; img-src * data:; base-uri 'self'; frame-ancestors 'none';"
      )
    }
  }

}
