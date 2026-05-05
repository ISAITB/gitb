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

import config.Configurations
import play.api.ApplicationLoader
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceApplicationLoader}

class CustomApplicationLoader extends GuiceApplicationLoader {

  override protected def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {
    // Load application configurations before the applications starts.
    Configurations.loadConfigurations()
    // Load proxy settings because Guice needs to read the system properties when wiring together classes such as Play's WSClient.
    setupProxy()
    // Allow WebJars to be injected into the ErrorHandler by allowing circular dependencies (which only occur in the error handler).
    super.builder(context).disableCircularProxies(false)
  }

  private def setupProxy(): Unit = {
    if (Configurations.PROXY_SERVER_ENABLED) {
      System.setProperty("http.proxyHost", Configurations.PROXY_SERVER_HOST)
      System.setProperty("http.proxyPort", Configurations.PROXY_SERVER_PORT.toString)
      System.setProperty("https.proxyHost", Configurations.PROXY_SERVER_HOST)
      System.setProperty("https.proxyPort", Configurations.PROXY_SERVER_PORT.toString)
      // Build the non-proxy hosts list, always excluding localhost and the internal testbed client host.
      val nonProxyHosts = {
        val automaticExclusions = scala.collection.mutable.LinkedHashSet("localhost", "127.*")
        try {
          val internalHost = new java.net.URI(Configurations.TESTBED_CLIENT_URL_INTERNAL).getHost
          if (internalHost != null) automaticExclusions += internalHost
        } catch {
          case _: Exception =>
            // Ignore.
        }
        val userDefined = Configurations.PROXY_SERVER_NON_PROXY_HOSTS
          .map(_.split('|').map(_.trim).filter(_.nonEmpty).toSeq)
          .getOrElse(Seq.empty)
        (automaticExclusions ++ userDefined).mkString("|")
      }
      System.setProperty("http.nonProxyHosts", nonProxyHosts)
      System.setProperty("https.nonProxyHosts", nonProxyHosts)
      if (Configurations.PROXY_SERVER_AUTH_ENABLED) {
        System.setProperty("http.proxyUser", Configurations.PROXY_SERVER_AUTH_USERNAME)
        System.setProperty("http.proxyPassword", Configurations.PROXY_SERVER_AUTH_PASSWORD)
      }
    }
  }

}