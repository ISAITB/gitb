package modules

import config.Configurations
import org.apache.commons.lang3.StringUtils
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.engine.savedrequest.DefaultSavedRequestHandler

/**
 * Custom saved request handler to make sure that:
 * - We only ever redirect to "/app" following a login.
 * - We make sure that the complete URL matches how we are expected to be called externally (to avoid problems when running behind a proxy).
 */
class CustomSavedRequestHandler extends DefaultSavedRequestHandler {

  override protected def getRequestedUrl(context: WebContext, sessionStore: SessionStore): String = {
    if (Configurations.TESTBED_HOME_LINK != "/") {
      val homeWithSlash = StringUtils.appendIfMissing(Configurations.TESTBED_HOME_LINK, "/")
      StringUtils.appendIfMissing(homeWithSlash, "app")
    } else {
      super.getRequestedUrl(context, sessionStore)
    }
  }

}
