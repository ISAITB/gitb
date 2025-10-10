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

package authentication

import models.ActualUserInfo
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.{ProfileManager, UserProfile}
import org.pac4j.play.PlayWebContext
import org.slf4j.LoggerFactory
import play.api.mvc.RequestHeader

abstract class BaseProfileResolver(playSessionStore: SessionStore) extends ProfileResolver {

  private val logger = LoggerFactory.getLogger(classOf[BaseProfileResolver])

  override def resolveUserInfo(request: RequestHeader): Option[ActualUserInfo] = {
    val webContext = new PlayWebContext(request)
    val profileManager = new ProfileManager(webContext, playSessionStore)
    val profile = profileManager.getProfile()
    val userInfo = if (profile.isEmpty) {
      logger.error("Lookup for a real user's data failed due to a missing profile.")
      Option.empty
    } else {
      val extractedInfo = extractUserInfo(profile.get())
      if (extractedInfo.isDefined) {
        if (extractedInfo.get.uid == null || extractedInfo.get.email == null || extractedInfo.get.name == null) {
          logger.error("User profile did not contain expected information: ID [{}], email [{}], name [{}]", extractedInfo.get.uid, extractedInfo.get.email, extractedInfo.get.name)
          Option.empty
        } else {
          extractedInfo
        }
      } else {
        logger.error("No user profile could be extracted")
        Option.empty
      }
    }
    userInfo
  }

  def extractUserInfo(profile: UserProfile): Option[ActualUserInfo]

}
