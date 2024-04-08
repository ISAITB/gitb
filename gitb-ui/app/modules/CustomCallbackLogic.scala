package modules

import org.pac4j.core.context.CallContext
import org.pac4j.core.engine.DefaultCallbackLogic
import org.pac4j.core.exception.http.{FoundAction, HttpAction}
import org.pac4j.core.util.{HttpActionHelper, Pac4jConstants}

/**
 * Custom callback logic to make sure that:
 * - We only ever redirect to the default home following a login ("/app").
 * - We make sure that the complete URL matches how we are expected to be called externally (to avoid problems when running behind a proxy).
 */
class CustomCallbackLogic extends DefaultCallbackLogic {

  override protected def redirectToOriginallyRequestedUrl(ctx: CallContext, defaultUrl: String): HttpAction = {
    // Clear session entry (if present).
    val optRequestedUrl = ctx.sessionStore().get(ctx.webContext(), Pac4jConstants.REQUESTED_URL)
    if (optRequestedUrl.isPresent) {
      ctx.sessionStore().set(ctx.webContext(), Pac4jConstants.REQUESTED_URL, null)
    }
    // Return a redirect to the default URL.
    HttpActionHelper.buildRedirectUrlAction(ctx.webContext(), new FoundAction(defaultUrl).getLocation)
  }

}
