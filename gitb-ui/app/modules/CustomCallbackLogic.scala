package modules

import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.engine.DefaultCallbackLogic
import org.pac4j.core.exception.http.{FoundAction, HttpAction}
import org.pac4j.core.util.{HttpActionHelper, Pac4jConstants}

/**
 * Custom callback logic to make sure that:
 * - We only ever redirect to the default home following a login ("/app").
 * - We make sure that the complete URL matches how we are expected to be called externally (to avoid problems when running behind a proxy).
 */
class CustomCallbackLogic extends DefaultCallbackLogic {

  override protected def redirectToOriginallyRequestedUrl(context: WebContext, sessionStore: SessionStore, defaultUrl: String): HttpAction = {
    // Clear session entry (if present).
    val optRequestedUrl = sessionStore.get(context, Pac4jConstants.REQUESTED_URL)
    if (optRequestedUrl.isPresent) {
      sessionStore.set(context, Pac4jConstants.REQUESTED_URL, null)
    }
    // Return a redirect to the default URL.
    HttpActionHelper.buildRedirectUrlAction(context, new FoundAction(defaultUrl).getLocation)
  }

}
