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

package com.gitb.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Authorizes the URIs that the engine is asked to look up (e.g. imports in validation artefacts, JSON-LD contexts, URI
 * inputs), guarding against server-side request forgery.
 * <p>
 * Only HTTP(S) URIs are ever authorized. URIs that resolve to public addresses are allowed freely, whereas URIs that
 * resolve to internal or reserved addresses (loopback, private ranges, link-local, cloud metadata, etc.) are only
 * allowed if they start with one of the whitelisted base URIs.
 * <p>
 * The whitelist is an operator-level setting (the {@value #ENV_ALLOWED_INTERNAL_URIS} environment variable, or the
 * {@value #PROPERTY_ALLOWED_INTERNAL_URIS} system property). It is never influenced by test cases or session inputs.
 */
public class UriAuthorizer {

    /**
     * The environment variable listing (comma-separated) the base URIs of internal addresses that may be looked up.
     */
    public static final String ENV_ALLOWED_INTERNAL_URIS = "ALLOWED_INTERNAL_URIS";
    /**
     * The system property that can be used instead of the environment variable (used by default if the environment variable is not set).
     */
    public static final String PROPERTY_ALLOWED_INTERNAL_URIS = "gitb.allowed.internal.uris";

    private static final Logger LOG = LoggerFactory.getLogger(UriAuthorizer.class);

    private static final List<CidrInfo> CIDR_BLACKLIST = List.of(
            // IPv4
            CidrInfo.from("127.0.0.0/8"),       // loopback
            CidrInfo.from("10.0.0.0/8"),        // RFC 1918
            CidrInfo.from("172.16.0.0/12"),     // RFC 1918
            CidrInfo.from("192.168.0.0/16"),    // RFC 1918
            CidrInfo.from("169.254.0.0/16"),    // link-local / IMDS
            CidrInfo.from("0.0.0.0/8"),         // "this" network
            CidrInfo.from("100.64.0.0/10"),     // shared address space (RFC 6598)
            CidrInfo.from("192.0.0.0/24"),      // IETF protocol assignments
            CidrInfo.from("198.18.0.0/15"),     // benchmarking
            CidrInfo.from("192.0.2.0/24"),      // TEST-NET-1
            CidrInfo.from("198.51.100.0/24"),   // TEST-NET-2
            CidrInfo.from("203.0.113.0/24"),    // TEST-NET-3
            CidrInfo.from("240.0.0.0/4"),       // reserved
            // IPv6
            CidrInfo.from("::1/128"),           // loopback
            CidrInfo.from("fd00::/8"),          // unique local
            CidrInfo.from("fe80::/10"),         // link-local
            CidrInfo.from("fc00::/7"),          // unique local (full range)
            CidrInfo.from("::/128"),            // unspecified
            CidrInfo.from("100::/64")           // discard prefix
    );

    private static volatile UriAuthorizer global;

    private final List<NormalizedUri> whitelist;
    private final String whitelistSettingName;

    /**
     * Constructor.
     *
     * @param whitelistedBaseUris The base URIs of the internal addresses that may be looked up (may be null or empty).
     * @throws IllegalArgumentException If a whitelisted base URI is not a valid, absolute HTTP(S) URI.
     */
    public UriAuthorizer(Collection<String> whitelistedBaseUris) {
        this(whitelistedBaseUris, ENV_ALLOWED_INTERNAL_URIS);
    }

    /**
     * Constructor.
     *
     * @param whitelistedBaseUris The base URIs of the internal addresses that may be looked up (may be null or empty).
     * @param whitelistSettingName The name of the setting from which the whitelist is configured (mentioned when advising on how to allow a blocked URI).
     * @throws IllegalArgumentException If a whitelisted base URI is not a valid, absolute HTTP(S) URI.
     */
    public UriAuthorizer(Collection<String> whitelistedBaseUris, String whitelistSettingName) {
        this.whitelistSettingName = whitelistSettingName;
        this.whitelist = whitelistedBaseUris == null ? List.of() : whitelistedBaseUris.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(UriAuthorizer::parseWhitelistEntry)
                .toList();
    }

    /**
     * Create an authorizer from a comma-separated list of whitelisted base URIs.
     *
     * @param commaSeparatedBaseUris The list (may be null or empty).
     * @return The authorizer.
     * @throws IllegalArgumentException If a whitelisted base URI is not a valid, absolute HTTP(S) URI.
     */
    public static UriAuthorizer parse(String commaSeparatedBaseUris) {
        if (commaSeparatedBaseUris == null) {
            return new UriAuthorizer(List.of());
        }
        return new UriAuthorizer(Arrays.asList(commaSeparatedBaseUris.split(",")));
    }

    /**
     * Get the authorizer to apply for the lookups made by the engine, as configured by the operator.
     * <p>
     * Unless set explicitly (see {@link #setGlobal(UriAuthorizer)}) this is determined from the environment.
     *
     * @return The authorizer.
     */
    public static UriAuthorizer global() {
        var authorizer = global;
        if (authorizer == null) {
            synchronized (UriAuthorizer.class) {
                authorizer = global;
                if (authorizer == null) {
                    String configured = System.getenv(ENV_ALLOWED_INTERNAL_URIS);
                    if (configured == null || configured.isBlank()) {
                        configured = System.getProperty(PROPERTY_ALLOWED_INTERNAL_URIS);
                    }
                    authorizer = parse(configured);
                    if (authorizer.whitelist.isEmpty()) {
                        LOG.info("No internal addresses are whitelisted for resource lookups (see {})", ENV_ALLOWED_INTERNAL_URIS);
                    } else {
                        LOG.info("Whitelisted internal addresses for resource lookups: {}", authorizer.whitelist.stream().map(NormalizedUri::toString).toList());
                    }
                    global = authorizer;
                }
            }
        }
        return authorizer;
    }

    /**
     * Explicitly set the authorizer to apply for the lookups made by the engine.
     *
     * @param authorizer The authorizer (null to determine it again from the environment).
     */
    public static void setGlobal(UriAuthorizer authorizer) {
        synchronized (UriAuthorizer.class) {
            global = authorizer;
        }
    }

    /**
     * Check that the provided URI can be looked up.
     *
     * @param uri The URI to check.
     * @throws UriNotAllowedException If the URI is not allowed.
     */
    public void check(URI uri) {
        if (uri == null || !uri.isAbsolute()) {
            throw new UriNotAllowedException("The provided URI [%s] is not allowed because it is not an absolute URI.".formatted(uri));
        }
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new UriNotAllowedException("The provided URI [%s] is not allowed because only http and https URIs can be looked up.".formatted(uri));
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UriNotAllowedException("The provided URI [%s] is not allowed because it has no valid host.".formatted(uri));
        }
        if (hostRequiresWhitelist(host)) {
            // Internal or reserved address: require an explicit whitelist match.
            var normalized = NormalizedUri.of(uri);
            if (whitelist.stream().noneMatch(entry -> entry.isPrefixOf(normalized))) {
                LOG.warn("Blocked lookup of URI [{}] that refers to an internal or reserved address. If using this URI was intended, consider whitelisting it by adding it to the {} setting.", uri, whitelistSettingName);
                throw new UriNotAllowedException("The provided URI [%s] is not allowed because it refers to an internal or reserved address.".formatted(uri));
            }
        }
    }

    /**
     * Check that the provided URI can be looked up.
     *
     * @param uri The URI to check.
     * @throws UriNotAllowedException If the URI is not allowed or cannot be parsed.
     */
    public void check(String uri) {
        URI parsed;
        try {
            parsed = URI.create(uri == null ? "" : uri.trim());
        } catch (IllegalArgumentException e) {
            throw new UriNotAllowedException("The provided URI [%s] is not valid.".formatted(uri));
        }
        check(parsed);
    }

    /**
     * Check whether the provided URI can be looked up.
     *
     * @param uri The URI to check.
     * @return The check result.
     */
    public boolean isAllowed(URI uri) {
        try {
            check(uri);
            return true;
        } catch (UriNotAllowedException e) {
            return false;
        }
    }

    private static NormalizedUri parseWhitelistEntry(String entry) {
        URI uri;
        try {
            uri = URI.create(entry);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid whitelisted URI [%s]".formatted(entry), e);
        }
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || uri.getHost() == null) {
            throw new IllegalArgumentException("Invalid whitelisted URI [%s]: an absolute http or https URI is expected".formatted(entry));
        }
        return NormalizedUri.of(uri);
    }

    private boolean hostRequiresWhitelist(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            // Fail-safe: unresolved hosts are treated as internal.
            return true;
        }
        // Fail-safe for mixed records: if any resolved address is internal/reserved, treat the host as internal/reserved.
        for (InetAddress address : addresses) {
            if (addressRequiresWhitelist(address)) {
                return true;
            }
        }
        return false;
    }

    private boolean addressRequiresWhitelist(InetAddress address) {
        if (address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isAnyLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        return CIDR_BLACKLIST.stream().anyMatch(cidr -> cidr.contains(address));
    }

    /**
     * A URI normalized for consistent comparisons.
     *
     * @param scheme The scheme (lowercase).
     * @param host The host (lowercase).
     * @param port The port (with the default port of the scheme made explicit).
     * @param path The path.
     */
    private record NormalizedUri(String scheme, String host, int port, String path) {

        static NormalizedUri of(URI uri) {
            URI normalised = uri.normalize();
            String scheme = normalised.getScheme() == null ? null : normalised.getScheme().toLowerCase(Locale.ROOT);
            String host = normalised.getHost() == null ? null : normalised.getHost().toLowerCase(Locale.ROOT);
            String path = normalised.getPath() == null ? "" : normalised.getPath();
            int port = normalised.getPort();
            if (port == -1 && scheme != null) {
                port = switch (scheme) {
                    case "https" -> 443;
                    case "http" -> 80;
                    default -> -1;
                };
            }
            return new NormalizedUri(scheme, host, port, path);
        }

        /**
         * Check whether this (whitelisted) URI is a prefix of the provided one. The path is compared by segment, so that
         * a whitelisted path "/api" matches "/api" and "/api/test" but not "/api-private".
         */
        boolean isPrefixOf(NormalizedUri input) {
            if (!Objects.equals(input.scheme, scheme) || !Objects.equals(input.host, host) || input.port != port) {
                return false;
            }
            if (path.isEmpty() || path.equals("/") || input.path.equals(path)) {
                return true;
            }
            String pathPrefix = path.endsWith("/") ? path : path + "/";
            return input.path.startsWith(pathPrefix);
        }

        @Override
        public String toString() {
            return "%s://%s:%s%s".formatted(scheme, host, port, path);
        }
    }

    /**
     * Helper record to record CIDR info.
     *
     * @param rangeAddressBytes The address bytes.
     * @param prefixLength The prefix length.
     */
    private record CidrInfo(byte[] rangeAddressBytes, int prefixLength) {

        static CidrInfo from(String cidr) {
            int slash = cidr.indexOf('/');
            try {
                return new CidrInfo(InetAddress.getByName(cidr.substring(0, slash)).getAddress(), Integer.parseInt(cidr.substring(slash + 1)));
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Unable to parse CIDR", e);
            }
        }

        boolean contains(InetAddress address) {
            byte[] addressBytes = address.getAddress();
            if (addressBytes.length != rangeAddressBytes.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (addressBytes[i] != rangeAddressBytes[i]) {
                    return false;
                }
            }
            if (remainingBits > 0) {
                int mask = 0xFF << (8 - remainingBits);
                return (addressBytes[fullBytes] & mask) == (rangeAddressBytes[fullBytes] & mask);
            }
            return true;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || (o instanceof CidrInfo other && prefixLength == other.prefixLength && Arrays.equals(rangeAddressBytes, other.rangeAddressBytes));
        }

        @Override
        public int hashCode() {
            return 31 * Integer.hashCode(prefixLength) + Arrays.hashCode(rangeAddressBytes);
        }

        @Override
        public String toString() {
            return "CidrInfo[rangeAddressBytes=" + Arrays.toString(rangeAddressBytes) + ", prefixLength=" + prefixLength + "]";
        }
    }

}
