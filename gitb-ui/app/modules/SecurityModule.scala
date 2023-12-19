package modules

import com.google.inject.{AbstractModule, Provides}
import config.Configurations
import config.Configurations.API_ROOT
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
    casConfiguration.setLoginUrl(Configurations.AUTHENTICATION_SSO_LOGIN_URL)
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
      .excludePath("/")
      .excludePath("/"+API_ROOT+"/notices/tbdefault")
      .excludeBranch("/"+API_ROOT+"/theme")
      .excludeBranch("/assets")
      .excludeBranch("/webjars")
      .excludeBranch("/template")
      .excludePath("/favicon.ico")
      .excludeBranch("/callback")
      .excludeBranch("/"+API_ROOT+"/repository/tests")
      .excludeBranch("/"+API_ROOT+"/repository/resource")
      .excludeBranch("/"+API_ROOT+"/rest")
      .excludeBranch("/badge")
    )
    config
  }
}
