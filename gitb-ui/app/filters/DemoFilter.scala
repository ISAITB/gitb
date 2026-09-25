/*
 * Copyright (C) 2026 European Union
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

package filters

import config.Configurations
import models.Constants
import org.apache.pekko.stream.Materializer
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.play.PlayWebContext
import persistence.cache.TokenCache
import play.api.mvc.{Filter, RequestHeader, Result, Session}
import play.api.mvc.Results.Redirect

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.MapHasAsScala

class DemoFilter @Inject() (implicit val mat: Materializer,
                            ec: ExecutionContext,
                            playSessionStore: SessionStore) extends Filter {

  /*
   * LOGIN_OPTION values that are only ever set from the welcome page's cards (see gitb-cookie-cleanup.js).
   * The "force" and "*_internal" variants are recorded from within the already-loaded Angular app and must
   * not trigger the demo session invalidation below.
   */
  private val welcomePageLoginOptions = Set("none", "link", "register", "demo", "migrate")

  override def apply(next: RequestHeader => Future[Result])
                    (requestHeader: RequestHeader): Future[Result] = {
    if (Configurations.DEMOS_ENABLED) {
      val loginOptionCookieValue = requestHeader.cookies.get("LOGIN_OPTION").map(_.value)
      if (isAppLoadRequest(requestHeader) && loginOptionCookieValue.exists(welcomePageLoginOptions.contains) && isDemoSession(requestHeader)) {
        /*
         * The user is revisiting the welcome page and selecting one of its options (e.g. "Log in") while
         * still holding a session for the demo account. Unlike normal accounts, the demo session must not
         * be silently resumed in this case, otherwise the user would always be reconnected as the demo user
         * regardless of the option they picked. Invalidate the demo session (access token + pac4j profile)
         * and let the browser re-request the app page with a clean session.
         */
        invalidateDemoSession(requestHeader)
        Future.successful(Redirect("%sapp".formatted(Configurations.WEB_CONTEXT_ROOT_WITH_SLASH)).withNewSession)
      } else if (loginOptionCookieValue.contains("demo")) {
        /*
         * Log in the user as the configured demo account.
         */
        val webContext = new PlayWebContext(requestHeader)
        val profileManager = new ProfileManager(webContext, playSessionStore)
        var createDemoProfile = false
        if (profileManager.isAuthenticated) {
          if (profileManager.getProfile.isEmpty || profileManager.getProfile.get().getId != Constants.DemoUserProfileIdentifier) {
            profileManager.removeProfiles()
            playSessionStore.destroySession(webContext)
            createDemoProfile = true
          }
        } else {
          createDemoProfile = true
        }
        if (createDemoProfile) {
          val userProfile = new CommonProfile()
          userProfile.setId(Constants.DemoUserProfileIdentifier)
          profileManager.save(true, userProfile, false)
          next(requestHeader).map { result =>
            result.withSession(Session(webContext.getNativeSession.data().asScala.toMap))
          }
        } else {
          next(requestHeader)
        }
      } else {
        next(requestHeader)
      }
    } else {
      next(requestHeader)
    }
  }

  private def isAppLoadRequest(requestHeader: RequestHeader): Boolean = {
    requestHeader.method == "GET" && requestHeader.path == "%sapp".formatted(Configurations.WEB_CONTEXT_ROOT_WITH_SLASH)
  }

  /** True if the current session (access token and/or pac4j profile) belongs to the configured demo account. */
  private def isDemoSession(requestHeader: RequestHeader): Boolean = {
    val demoViaAccessToken = requestHeader.session.get(Constants.AccessTokenKey).exists { token =>
      TokenCache.checkAccessToken(token).contains(Configurations.DEMOS_ACCOUNT)
    }
    demoViaAccessToken || isDemoProfile(requestHeader)
  }

  private def isDemoProfile(requestHeader: RequestHeader): Boolean = {
    val webContext = new PlayWebContext(requestHeader)
    val profileManager = new ProfileManager(webContext, playSessionStore)
    profileManager.isAuthenticated && profileManager.getProfile.isPresent && profileManager.getProfile.get().getId == Constants.DemoUserProfileIdentifier
  }

  private def invalidateDemoSession(requestHeader: RequestHeader): Unit = {
    requestHeader.session.get(Constants.AccessTokenKey).foreach(TokenCache.deleteOAthToken)
    val webContext = new PlayWebContext(requestHeader)
    val profileManager = new ProfileManager(webContext, playSessionStore)
    profileManager.removeProfiles()
    playSessionStore.destroySession(webContext)
  }
}

