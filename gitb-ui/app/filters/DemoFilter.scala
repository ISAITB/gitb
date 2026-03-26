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

import models.Constants
import org.apache.pekko.stream.Materializer
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.play.PlayWebContext
import play.api.mvc.{Filter, RequestHeader, Result, Session}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.MapHasAsScala

class DemoFilter @Inject() (implicit val mat: Materializer,
                            ec: ExecutionContext,
                            playSessionStore: SessionStore) extends Filter {

  override def apply(next: RequestHeader => Future[Result])
                    (requestHeader: RequestHeader): Future[Result] = {
    if (requestHeader.cookies.exists(c => c.name == "LOGIN_OPTION" && c.value == "demo")) {
      val webContext = new PlayWebContext(requestHeader)
      val profileManager = new ProfileManager(webContext, playSessionStore)
      var createDemoProfile = false
      if (profileManager.isAuthenticated) {
        if (profileManager.getProfile.isEmpty || profileManager.getProfile.get().getId != Constants.DemoUserProfileIdentifier) {
          profileManager.removeProfiles()
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
  }
}

