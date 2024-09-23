package modules

import controllers.util.ResponseConstructor
import org.pac4j.core.context.{CallContext, Cookie}
import org.pac4j.core.engine.DefaultCallbackLogic
import org.pac4j.core.exception.http.{FoundAction, HttpAction}
import org.pac4j.core.util.{HttpActionHelper, Pac4jConstants}
import play.api.mvc.Cookie.SameSite

/**
 * Custom callback logic to make sure that:
 * - We only ever redirect to the default home following a login ("/app").
 * - We make sure that the complete URL matches how we are expected to be called externally (to avoid problems when running behind a proxy).
 */
class CustomCallbackLogic extends DefaultCallbackLogic {

  override protected def redirectToOriginallyRequestedUrl(ctx: CallContext, defaultUrl: String): HttpAction = {
    // Clear session entry (if present).
    val optRequestedUrl = ctx.sessionStore().get(ctx.webContext(), Pac4jConstants.REQUESTED_URL)
    if (optRequestedUrl.isPresent && optRequestedUrl.get.isInstanceOf[String]) {
      val requestedUrl = optRequestedUrl.get.asInstanceOf[String]
      ctx.sessionStore().set(ctx.webContext(), Pac4jConstants.REQUESTED_URL, null)
      // Add in a cookie the originally requested URL to allow the frontend client to perform correct navigation.
      val playCookie = ResponseConstructor.createRequestedUrlCookie(requestedUrl)
      val cookie = new Cookie(playCookie.name, playCookie.value)
      cookie.setHttpOnly(playCookie.httpOnly)
      cookie.setSameSitePolicy(playCookie.sameSite.getOrElse(SameSite.Strict).value)
      cookie.setSecure(playCookie.secure)
      ctx.webContext().addResponseCookie(cookie)
    }
    // Return a redirect to the default URL.
    HttpActionHelper.buildRedirectUrlAction(ctx.webContext(), new FoundAction(defaultUrl).getLocation)
  }

}
