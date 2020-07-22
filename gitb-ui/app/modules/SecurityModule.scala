package modules

import com.google.inject.{AbstractModule, Provides}
import config.Configurations
import ecas.ExtendedCasConfiguration
import org.pac4j.cas.client.{CasClient, CasProxyReceptor}
import org.pac4j.cas.config.CasProtocol
import org.pac4j.core.client.Clients
import org.pac4j.core.client.direct.AnonymousClient
import org.pac4j.core.config.Config
import org.pac4j.core.http.ajax.DefaultAjaxRequestResolver
import org.pac4j.core.matching.PathMatcher
import org.pac4j.play.http.DefaultHttpActionAdapter
import org.pac4j.play.store.{PlayCacheSessionStore, PlaySessionStore}
import org.pac4j.play.{CallbackController, LogoutController}

class SecurityModule extends AbstractModule {

  private val CLIENT_NAME: String = "euLoginCASClient"

  override def configure(): Unit = {
    // Make sure the configuration is loaded
    Configurations.loadConfigurations()

    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheSessionStore])

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
  def provideCasClient(casProxyReceptor: CasProxyReceptor) = {
    val casConfiguration = new ExtendedCasConfiguration()
    casConfiguration.setLoginUrl(Configurations.AUTHENTICATION_SSO_LOGIN_URL)
    if (Configurations.AUTHENTICATION_SSO_CAS_VERSION == 2) {
      casConfiguration.setProtocol(CasProtocol.CAS20)
    } else {
      casConfiguration.setProtocol(CasProtocol.CAS30)
    }
    casConfiguration.setProxyReceptor(casProxyReceptor)
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
    config.addAuthorizer("any", new BasicAuthorizer[Nothing])
    config.addMatcher("excludedPath", new PathMatcher().excludePath("/"))
    config.setHttpActionAdapter(new DefaultHttpActionAdapter())
    config
  }
}
