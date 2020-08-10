package modules

import com.google.inject.{AbstractModule, Provides}
import config.Configurations
import ecas.ExtendedCasConfiguration
import org.pac4j.cas.client.{CasClient, CasProxyReceptor}
import org.pac4j.cas.config.CasProtocol
import org.pac4j.core.authorization.authorizer.IsAuthenticatedAuthorizer
import org.pac4j.core.client.Clients
import org.pac4j.core.client.direct.AnonymousClient
import org.pac4j.core.config.Config
import org.pac4j.core.http.ajax.DefaultAjaxRequestResolver
import org.pac4j.core.matching.matcher.PathMatcher
import org.pac4j.core.profile.UserProfile
import org.pac4j.play.http.PlayHttpActionAdapter
import org.pac4j.play.store.PlaySessionStore
import org.pac4j.play.{CallbackController, LogoutController}
import play.cache.SyncCacheApi

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
    bind(classOf[PlaySessionStore]).toInstance(playCacheSessionStore)

    // callback
    val callbackController = new CallbackController()
    callbackController.setDefaultUrl("/app")
    callbackController.setMultiProfile(true)
    bind(classOf[CallbackController]).toInstance(callbackController)

    // logout
    val logoutController = new LogoutController()
    logoutController.setDefaultUrl("/")
    bind(classOf[LogoutController]).toInstance(logoutController)
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
    casClient.setAjaxRequestResolver(new DefaultAjaxRequestResolver)
    casClient
  }

  @Provides
  def provideConfig(casClient: CasClient, casProxyReceptor: CasProxyReceptor): Config = {
    var clients: Clients = null
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      clients = new Clients(Configurations.AUTHENTICATION_SSO_CALLBACK_URL, casClient, casProxyReceptor)
    } else {
      val anonymoucClient = new AnonymousClient()
      anonymoucClient.setName(CLIENT_NAME)
      clients = new Clients(anonymoucClient)
    }
    val config = new Config(clients)
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      config.addAuthorizer("_authenticated_", new IsAuthenticatedAuthorizer[UserProfile])
    } else {
      config.addAuthorizer("_authenticated_", new BasicAuthorizer[Nothing])
    }
    config.setHttpActionAdapter(new PlayHttpActionAdapter())
    config.addMatcher("excludedPath", new PathMatcher()
      .excludePath("/")
      .excludePath("/notices/tbdefault")
      .excludePath("/initdata")
      .excludePath("/favicon.ico")
      .excludeBranch("/theme")
      .excludeBranch("/assets")
      .excludeBranch("/webjars")
      .excludeBranch("/template")
      .excludeBranch("/callback")
      .excludeBranch("/repository/tests")
      .excludeBranch("/repository/resource")
    )
    config
  }
}
