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

package utils

import com.gitb.utils.{UriNotAllowedException, UriAuthorizer => CoreUriAuthorizer}
import config.Configurations
import config.Configurations.TESTSUITE_DEPLOY_ALLOWED_URIS_NAME
import exceptions.UnacceptableUriException
import org.slf4j.{Logger, LoggerFactory}

import java.net.URI
import scala.jdk.CollectionConverters._
import scala.util.Try

/**
 * Validates that a URI is safe to fetch from, guarding against SSRF attacks.
 *
 * Public/external addresses are allowed freely.  Internal or reserved addresses (loopback,
 * RFC 1918, link-local, cloud IMDS, etc.) are blocked unless the URI starts with one of the
 * base URIs listed in [[Configurations.TESTSUITE_DEPLOY_ALLOWED_URIS]].
 *
 * The checks are those of [[com.gitb.utils.UriAuthorizer]] (gitb-core), which is also used by gitb-srv
 * and mirrors ImportedUriAuthorizer in validation-commons.
 */
object UriAuthorizer {

  private final val logger: Logger = LoggerFactory.getLogger(getClass)

  /**
   * Assert that the given URI is safe to fetch from.
   *
   * @param uri The URI to validate.
   * @throws UnacceptableUriException if the URI is disallowed.
   */
  def assertAllowed(uri: URI): Unit = {
    if (!uri.isAbsolute) return
    try {
      authorizer().check(uri)
    } catch {
      case e: UriNotAllowedException => throw UnacceptableUriException(e.getMessage)
    }
  }

  /**
   * Assert that the given URI string is safe to fetch from.
   *
   * @param uriStr The URI string to validate.
   * @throws UnacceptableUriException if the URI is disallowed or cannot be parsed.
   */
  def assertAllowed(uriStr: String): Unit = {
    val uri = try {
      URI.create(uriStr)
    } catch {
      case _: IllegalArgumentException =>
        throw UnacceptableUriException(s"The provided URI is not valid.")
    }
    assertAllowed(uri)
  }

  /**
   * Create the authorizer considering the configured whitelist. Entries that cannot be used are ignored (and
   * reported) rather than making all lookups fail.
   */
  private def authorizer(): CoreUriAuthorizer = {
    val validEntries = Configurations.TESTSUITE_DEPLOY_ALLOWED_URIS.filter { entry =>
      val valid = Try(new CoreUriAuthorizer(List(entry).asJava)).isSuccess
      if (!valid) {
        logger.warn("Ignoring invalid URI [{}] set in the {} property. An absolute http or https URI is expected.", entry, TESTSUITE_DEPLOY_ALLOWED_URIS_NAME)
      }
      valid
    }
    new CoreUriAuthorizer(validEntries.toList.asJava, TESTSUITE_DEPLOY_ALLOWED_URIS_NAME)
  }

}
