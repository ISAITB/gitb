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

import config.Configurations
import config.Configurations.TESTSUITE_DEPLOY_ALLOWED_URIS_NAME
import exceptions.UnacceptableUriException
import org.slf4j.{Logger, LoggerFactory}

import java.net.{InetAddress, URI, UnknownHostException}

/**
 * Validates that a URI is safe to fetch from, guarding against SSRF attacks.
 *
 * Public/external addresses are allowed freely.  Internal or reserved addresses (loopback,
 * RFC 1918, link-local, cloud IMDS, etc.) are blocked unless the URI starts with one of the
 * base URIs listed in [[Configurations.TESTSUITE_DEPLOY_ALLOWED_URIS]].
 *
 * The logic mirrors ImportedUriAuthorizer in validation-commons.
 */
object UriAuthorizer {

  // CIDR blacklist records: (network address bytes, prefix length)
  private case class CidrInfo(addressBytes: Array[Byte], prefixLength: Int)

  private final val logger: Logger = LoggerFactory.getLogger(getClass)

  private val CIDR_BLACKLIST: List[CidrInfo] = List(
    // IPv4
    cidr("127.0.0.0/8"),       // loopback
    cidr("10.0.0.0/8"),        // RFC 1918
    cidr("172.16.0.0/12"),     // RFC 1918
    cidr("192.168.0.0/16"),    // RFC 1918
    cidr("169.254.0.0/16"),    // link-local / cloud IMDS (169.254.169.254)
    cidr("0.0.0.0/8"),         // "this" network
    cidr("100.64.0.0/10"),     // shared address space (RFC 6598 / CGNAT)
    cidr("192.0.0.0/24"),      // IETF protocol assignments
    cidr("198.18.0.0/15"),     // benchmarking
    cidr("192.0.2.0/24"),      // TEST-NET-1
    cidr("198.51.100.0/24"),   // TEST-NET-2
    cidr("203.0.113.0/24"),    // TEST-NET-3
    cidr("240.0.0.0/4"),       // reserved
    // IPv6
    cidr("::1/128"),           // loopback
    cidr("fd00::/8"),          // unique local
    cidr("fe80::/10"),         // link-local
    cidr("fc00::/7"),          // unique local (full range)
    cidr("::/128"),            // unspecified
    cidr("100::/64")           // discard prefix
  )

  private def cidr(notation: String): CidrInfo = {
    val slash = notation.lastIndexOf('/')
    val prefixLength = notation.substring(slash + 1).toInt
    val addressBytes = InetAddress.getByName(notation.substring(0, slash)).getAddress
    CidrInfo(addressBytes, prefixLength)
  }

  /**
   * Assert that the given URI is safe to fetch from.
   *
   * @param uri The URI to validate.
   * @throws UnacceptableUriException if the URI is disallowed.
   */
  def assertAllowed(uri: URI): Unit = {
    if (!uri.isAbsolute) return
    val scheme = uri.getScheme
    if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
      throw UnacceptableUriException(s"The provided URI uses a scheme that is not allowed. Only http and https URIs are accepted.")
    }
    val host = uri.getHost
    if (host == null || host.isBlank) {
      throw UnacceptableUriException(s"The provided URI does not contain a valid host.")
    }
    if (hostRequiresWhitelist(host)) {
      // Internal/reserved host: must match one of the configured whitelist base URIs.
      val uriStr = normalizeUri(uri)
      val allowed = Configurations.TESTSUITE_DEPLOY_ALLOWED_URIS.exists { base =>
        uriStr.startsWith(normalizeBase(base))
      }
      if (!allowed) {
        logger.warn("Blocked access to a disallowed URI. If using this URI was intended, consider whitelisting it by adding it to the {} property.", TESTSUITE_DEPLOY_ALLOWED_URIS_NAME)
        throw UnacceptableUriException(s"The provided URI points to an address that is not allowed.")
      }
    }
    // Public address: allowed freely.
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

  /** Check whether the host resolves to any internal/reserved address. */
  private def hostRequiresWhitelist(host: String): Boolean = {
    val addresses: Array[InetAddress] = try {
      InetAddress.getAllByName(host)
    } catch {
      case _: UnknownHostException =>
        // Fail-safe: unresolvable host is treated as internal/risky.
        return true
    }
    // Fail-safe for DNS-rebinding: if ANY resolved address is internal, treat the whole host as internal.
    addresses.exists(addressRequiresWhitelist)
  }

  private def addressRequiresWhitelist(address: InetAddress): Boolean = {
    if (address.isLoopbackAddress ||
        address.isSiteLocalAddress ||
        address.isLinkLocalAddress ||
        address.isAnyLocalAddress ||
        address.isMulticastAddress) {
      return true
    }
    isInBlacklist(address)
  }

  private def isInBlacklist(address: InetAddress): Boolean = {
    CIDR_BLACKLIST.exists(cidr => isInCidrRange(address, cidr))
  }

  private def isInCidrRange(address: InetAddress, cidr: CidrInfo): Boolean = {
    val addrBytes = address.getAddress
    if (addrBytes.length != cidr.addressBytes.length) return false
    val fullBytes = cidr.prefixLength / 8
    val remainingBits = cidr.prefixLength % 8
    for (i <- 0 until fullBytes) {
      if (addrBytes(i) != cidr.addressBytes(i)) return false
    }
    if (remainingBits > 0) {
      val mask = (0xFF << (8 - remainingBits)) & 0xFF
      (addrBytes(fullBytes) & mask) == (cidr.addressBytes(fullBytes) & mask)
    } else {
      true
    }
  }

  /** Normalize a URI to a lowercase scheme+host (with default port stripped) + path for prefix matching. */
  private def normalizeUri(uri: URI): String = {
    normalizeBase(uri.toString)
  }

  private def normalizeBase(uriStr: String): String = {
    // Lowercase, strip default port (80 for http, 443 for https).
    val lower = uriStr.toLowerCase
    lower
      .replaceAll("^http://([^/:]+):80(/|$)", "http://$1$2")
      .replaceAll("^https://([^/:]+):443(/|$)", "https://$1$2")
  }

}
