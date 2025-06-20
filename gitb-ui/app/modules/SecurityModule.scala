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

import com.google.inject.{AbstractModule, Provides}
import config.Configurations
import config.Configurations.{API_ROOT, WEB_CONTEXT_ROOT, WEB_CONTEXT_ROOT_WITH_SLASH}
import ecas.ExtendedCasConfiguration
import org.apache.commons.lang3.StringUtils
import org.pac4j.cas.client.{CasClient, CasProxyReceptor}
import org.pac4j.cas.config.CasProtocol
import org.pac4j.core.authorization.authorizer.IsAuthenticatedAuthorizer
import org.pac4j.core.client.Clients
import org.pac4j.core.client.direct.AnonymousClient
import org.pac4j.core.config.Config
import org.pac4j.core.context.FrameworkParameters
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.http.ajax.DefaultAjaxRequestResolver
import org.pac4j.core.http.callback.QueryParameterCallbackUrlResolver
import org.pac4j.core.matching.matcher.PathMatcher
import org.pac4j.core.util.Pac4jConstants
import org.pac4j.play.http.PlayHttpActionAdapter
import org.pac4j.play.{CallbackController, LogoutController}
import play.cache.SyncCacheApi

import java.util

class SecurityModule extends AbstractModule {

  private val CLIENT_NAME: String = "euLoginCASClient"

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

  @Provides
  def provideCasProxyReceptor() = {
    val casProxyReceptor: CasProxyReceptor = new CasProxyReceptor()
    casProxyReceptor
  }

  @Provides
  def provideCasClient() = {
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

  @Provides
  def provideCallbackUrlResolver() = {
    val params = new util.HashMap[String, String]()
    params.put(Pac4jConstants.DEFAULT_FORCE_CLIENT_PARAMETER, CLIENT_NAME)
    val resolver = new QueryParameterCallbackUrlResolver(params)
    resolver
  }

  @Provides
  def provideConfig(casClient: CasClient, casProxyReceptor: CasProxyReceptor, callbackUrlResolver: QueryParameterCallbackUrlResolver, sessionStore: SessionStore): Config = {
    var clients: Clients = null
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      clients = new Clients(Configurations.AUTHENTICATION_SSO_CALLBACK_URL, casClient, casProxyReceptor)
      clients.setCallbackUrlResolver(callbackUrlResolver)
    } else {
      val anonymousClient = new AnonymousClient()
      anonymousClient.setName(CLIENT_NAME)
      clients = new Clients(anonymousClient)
    }
    val config = new Config(clients)
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      config.addAuthorizer("_authenticated_", new IsAuthenticatedAuthorizer)
    } else {
      config.addAuthorizer("_authenticated_", new BasicAuthorizer)
    }
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
