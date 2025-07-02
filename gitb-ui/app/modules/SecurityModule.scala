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

import authentication.builtin.BasicAuthorizer
import authentication.ecas.ExtendedCasConfiguration
import authentication.{BaseProfileResolver, CustomCallbackLogic, CustomPlayEhCacheSessionStore, ProfileResolver}
import com.google.inject.{AbstractModule, Provides}
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import config.Configurations
import config.Configurations._
import models.{ActualUserInfo, Constants}
import org.apache.commons.lang3.StringUtils
import org.pac4j.cas.client.{CasClient, CasProxyReceptor}
import org.pac4j.cas.config.CasProtocol
import org.pac4j.core.authorization.authorizer.{Authorizer, IsAuthenticatedAuthorizer}
import org.pac4j.core.client.Clients
import org.pac4j.core.client.direct.AnonymousClient
import org.pac4j.core.config.Config
import org.pac4j.core.context.FrameworkParameters
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.http.ajax.DefaultAjaxRequestResolver
import org.pac4j.core.http.callback.{CallbackUrlResolver, NoParameterCallbackUrlResolver, QueryParameterCallbackUrlResolver}
import org.pac4j.core.matching.matcher.PathMatcher
import org.pac4j.core.profile.UserProfile
import org.pac4j.core.util.Pac4jConstants
import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.oidc.profile.OidcProfile
import org.pac4j.play.http.PlayHttpActionAdapter
import org.pac4j.play.{CallbackController, LogoutController}
import play.cache.SyncCacheApi

import java.util
import javax.inject.Singleton

class SecurityModule extends AbstractModule {

  private val CLIENT_NAME: String = "activeClient"

  override def configure(): Unit = {
    // Make sure the configuration is loaded
    Configurations.loadConfigurations()

    // Session store and idle/max session timeouts
    val playCacheSessionStore = new CustomPlayEhCacheSessionStore(getProvider(classOf[SyncCacheApi]))
    if (Configurations.AUTHENTICATION_SESSION_MAX_IDLE_TIME < 0) {
      playCacheSessionStore.setTimeout(0) // Infinite.
    } else {
      playCacheSessionStore.setTimeout(Configurations.AUTHENTICATION_SESSION_MAX_IDLE_TIME)
    }
    if (Configurations.AUTHENTICATION_SESSION_MAX_TOTAL_TIME < 0) {
      playCacheSessionStore.setMaxTimeout(0) // Infinite.
    } else {
      playCacheSessionStore.setMaxTimeout(Configurations.AUTHENTICATION_SESSION_MAX_TOTAL_TIME)
    }
    bind(classOf[SessionStore]).toInstance(playCacheSessionStore)

    // callback
    val callbackController = new CallbackController()
    callbackController.setDefaultUrl(buildDefaultCallbackUrl())
    bind(classOf[CallbackController]).toInstance(callbackController)

    // logout
    val logoutController = new LogoutController()
    logoutController.setDefaultUrl(StringUtils.appendIfMissing(Configurations.TESTBED_HOME_LINK, "/"))
    bind(classOf[LogoutController]).toInstance(logoutController)
  }

  private def buildDefaultCallbackUrl(): String = {
    val homeWithSlash = StringUtils.appendIfMissing(Configurations.TESTBED_HOME_LINK, "/")
    StringUtils.appendIfMissing(homeWithSlash, "app")
  }

  private def createCasClient(): CasClient = {
    val casConfiguration = new ExtendedCasConfiguration()
    // The login URL is used for the initial redirect.
    casConfiguration.setLoginUrl(buildLoginUrl())
    // The prefix URL will be used to build the ticket validation URL.
    casConfiguration.setPrefixUrl(buildPrefixUrl())
    if (Configurations.AUTHENTICATION_SSO_CAS_VERSION == 2) {
      casConfiguration.setProtocol(CasProtocol.CAS20)
    } else {
      casConfiguration.setProtocol(CasProtocol.CAS30)
    }
    val casClient = new CasClient(casConfiguration)
    casClient.setName(CLIENT_NAME)
    casClient.setMultiProfile(true)
    casClient.setAjaxRequestResolver(new DefaultAjaxRequestResolver)
    casClient
  }

  private def createOidcClient(): OidcClient = {
    val oidcConfiguration = new OidcConfiguration()
    oidcConfiguration.setClientId(Configurations.AUTHENTICATION_CLIENT_ID)
    oidcConfiguration.setSecret(Configurations.AUTHENTICATION_CLIENT_SECRET)
    oidcConfiguration.setDiscoveryURI(Configurations.AUTHENTICATION_DISCOVERY_URI)
    oidcConfiguration.setClientAuthenticationMethod(ClientAuthenticationMethod.parse(Configurations.AUTHENTICATION_CLIENT_AUTHENTICATION_METHOD))
    oidcConfiguration.setUseNonce(Configurations.AUTHENTICATION_USE_NONCE)
    oidcConfiguration.setScope(Configurations.AUTHENTICATION_SCOPE)
    if (Configurations.AUTHENTICATION_RESPONSE_TYPE.isDefined) {
      oidcConfiguration.setResponseType(Configurations.AUTHENTICATION_RESPONSE_TYPE.get)
    }
    if (Configurations.AUTHENTICATION_RESPONSE_MODE.isDefined) {
      oidcConfiguration.setResponseMode(Configurations.AUTHENTICATION_RESPONSE_MODE.get)
    }
    val oidcClient = new OidcClient(oidcConfiguration)
    oidcClient.setName(CLIENT_NAME)
    oidcClient
  }

  private def buildLoginUrl(): String = {
    // If an authentication level is specified we append it to the login URL.
    if (Configurations.AUTHENTICATION_SSO_AUTHENTICATION_LEVEL.nonEmpty) {
      "%s?%s=%s".formatted(Configurations.AUTHENTICATION_SSO_LOGIN_URL, Configurations.AUTHENTICATION_SSO_AUTHENTICATION_LEVEL_PARAMETER, Configurations.AUTHENTICATION_SSO_AUTHENTICATION_LEVEL.get)
    } else {
      Configurations.AUTHENTICATION_SSO_LOGIN_URL
    }
  }

  private def buildPrefixUrl(): String = {
    // The prefix URL is the login URL without the "/login" suffix.
    Configurations.AUTHENTICATION_SSO_PREFIX_URL.getOrElse(Configurations.AUTHENTICATION_SSO_LOGIN_URL.replaceFirst("/login$", "/"))
  }

  private def createClients(): Clients = {
    val clients = if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      if (Configurations.AUTHENTICATION_SSO_TYPE == Constants.SsoTypeEcas) {
        new Clients(Configurations.AUTHENTICATION_SSO_CALLBACK_URL, createCasClient(), new CasProxyReceptor())
      } else if (Configurations.AUTHENTICATION_SSO_TYPE == Constants.SsoTypeOidc) {
        new Clients(Configurations.AUTHENTICATION_SSO_CALLBACK_URL, createOidcClient())
      } else {
        throw new IllegalStateException("Unsupported value for AUTHENTICATION_SSO_TYPE: " + Configurations.AUTHENTICATION_SSO_TYPE)
      }
    } else {
      val anonymousClient = new AnonymousClient()
      anonymousClient.setName(CLIENT_NAME)
      new Clients(anonymousClient)
    }
    clients.setCallbackUrlResolver(createCallbackUrlResolver())
    clients
  }

  private def createCallbackUrlResolver(): CallbackUrlResolver = {
    if (AUTHENTICATION_SSO_ENABLED && AUTHENTICATION_SSO_TYPE == Constants.SsoTypeEcas) {
      val params = new util.HashMap[String, String]()
      params.put(Pac4jConstants.DEFAULT_FORCE_CLIENT_PARAMETER, CLIENT_NAME)
      new QueryParameterCallbackUrlResolver(params)
    } else {
      new NoParameterCallbackUrlResolver()
    }
  }

  private def createAuthorizer(): Authorizer = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      new IsAuthenticatedAuthorizer
    } else {
      new BasicAuthorizer
    }
  }

  @Provides
  @Singleton
  def provideProfileResolver(playSessionStore: SessionStore): ProfileResolver = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      if (Configurations.AUTHENTICATION_SSO_TYPE == Constants.SsoTypeEcas) {
        new BaseProfileResolver(playSessionStore) {
          override def extractUserInfo(profile: UserProfile): Option[ActualUserInfo] = {
            Option(profile.getAttributes).map { userAttributes =>
              ActualUserInfo.fromAttributes(
                uid = profile.getId,
                email = userAttributes.get(Constants.UserAttributeEmail).asInstanceOf[String],
                name = None,
                firstName = Option(userAttributes.get(Constants.UserAttributeFirstName).asInstanceOf[String]),
                lastName = Option(userAttributes.get(Constants.UserAttributeLastName).asInstanceOf[String])
              )
            }
          }
        }
      } else if (Configurations.AUTHENTICATION_SSO_TYPE == Constants.SsoTypeOidc) {
        new BaseProfileResolver(playSessionStore) {
          override def extractUserInfo(profile: UserProfile): Option[ActualUserInfo] = {
            Option(profile.asInstanceOf[OidcProfile]).map { oidcProfile =>
              ActualUserInfo.fromAttributes(
                uid = oidcProfile.getId,
                email = oidcProfile.getEmail,
                name = Option(oidcProfile.getDisplayName),
                firstName = Option(oidcProfile.getFirstName),
                lastName = Option(oidcProfile.getFamilyName)
              )
            }
          }
        }
      } else {
        throw new IllegalStateException("Unsupported value for AUTHENTICATION_SSO_TYPE: " + Configurations.AUTHENTICATION_SSO_TYPE)
      }
    } else {
      new BaseProfileResolver(playSessionStore) {
        override def extractUserInfo(profile: UserProfile): Option[ActualUserInfo] = {
          // Not applicable
          None
        }
      }
    }
  }

  @Provides
  def provideConfig(sessionStore: SessionStore): Config = {
    val config = new Config(createClients())
    config.addAuthorizer("_authenticated_", createAuthorizer())
    config.setCallbackLogic(new CustomCallbackLogic)
    config.setSessionStoreFactory((_: FrameworkParameters) => sessionStore);
    config.setHttpActionAdapter(new PlayHttpActionAdapter())
    config.addMatcher("excludedPath", new PathMatcher()
      .excludePath("%s".formatted(WEB_CONTEXT_ROOT))
      .excludePath("%s".formatted(WEB_CONTEXT_ROOT_WITH_SLASH))
      .excludePath("%s/notices/tbdefault".formatted(API_ROOT))
      .excludeBranch("%s/theme".formatted(API_ROOT))
      .excludeBranch("%sassets".formatted(WEB_CONTEXT_ROOT_WITH_SLASH))
      .excludeBranch("%swebjars".formatted(WEB_CONTEXT_ROOT_WITH_SLASH))
      .excludeBranch("%stemplate".formatted(WEB_CONTEXT_ROOT_WITH_SLASH))
      .excludePath("%sfavicon.ico".formatted(WEB_CONTEXT_ROOT_WITH_SLASH))
      .excludeBranch("%scallback".formatted(WEB_CONTEXT_ROOT_WITH_SLASH))
      .excludeBranch("%s/repository/tests".formatted(API_ROOT))
      .excludeBranch("%s/repository/resource".formatted(API_ROOT))
      .excludeBranch("%s/rest".formatted(API_ROOT))
      .excludePath("%s/healthcheck".formatted(API_ROOT))
      .excludeBranch("%sbadge".formatted(WEB_CONTEXT_ROOT_WITH_SLASH))
      .excludeBranch("%ssystemResources".formatted(WEB_CONTEXT_ROOT_WITH_SLASH))
    )
    config
  }

}
