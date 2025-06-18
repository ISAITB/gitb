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
      if (playCookie.isDefined) {
        val cookie = new Cookie(playCookie.get.name, playCookie.get.value)
        cookie.setHttpOnly(playCookie.get.httpOnly)
        cookie.setSameSitePolicy(playCookie.get.sameSite.getOrElse(SameSite.Strict).value)
        cookie.setSecure(playCookie.get.secure)
        cookie.setPath(playCookie.get.path)
        ctx.webContext().addResponseCookie(cookie)
      }
    }
    // Return a redirect to the default URL.
    HttpActionHelper.buildRedirectUrlAction(ctx.webContext(), new FoundAction(defaultUrl).getLocation)
  }

}
