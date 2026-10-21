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

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UriAuthorizerTest {

    private final UriAuthorizer noWhitelist = new UriAuthorizer(List.of());

    @Test
    void publicAddressIsAllowed() {
        assertDoesNotThrow(() -> noWhitelist.check("http://8.8.8.8/schema.xsd"));
        assertDoesNotThrow(() -> noWhitelist.check("https://8.8.8.8:8443/schema.xsd"));
    }

    @Test
    void internalAndReservedAddressesAreBlocked() {
        for (String uri : List.of(
                "http://127.0.0.1/a",
                "http://localhost/a",
                "http://10.1.2.3/a",
                "http://172.16.0.1/a",
                "http://192.168.1.1/a",
                "http://169.254.169.254/latest/meta-data/",
                "http://0.0.0.0/a",
                "http://100.64.0.1/a",
                "http://[::1]/a",
                "http://[fd00::1]/a",
                "http://[fe80::1]/a",
                "http://[::ffff:127.0.0.1]/a",
                "http://2130706433/a",
                "http://nonexistent.invalid/a"
        )) {
            assertThrows(UriNotAllowedException.class, () -> noWhitelist.check(uri), uri);
        }
    }

    @Test
    void otherSchemesAreBlocked() {
        for (String uri : List.of("file:///etc/passwd", "jar:file:///tmp/a.jar!/x", "ftp://8.8.8.8/a", "classpath:a/b", "data:text/plain,abc", "gopher://8.8.8.8/")) {
            assertThrows(UriNotAllowedException.class, () -> noWhitelist.check(uri), uri);
        }
    }

    @Test
    void relativeAndInvalidUrisAreBlocked() {
        assertThrows(UriNotAllowedException.class, () -> noWhitelist.check("relative/path.xsd"));
        assertThrows(UriNotAllowedException.class, () -> noWhitelist.check("http://"));
        assertThrows(UriNotAllowedException.class, () -> noWhitelist.check("http://a b/"));
        assertThrows(UriNotAllowedException.class, () -> noWhitelist.check((URI) null));
        assertThrows(UriNotAllowedException.class, () -> noWhitelist.check((String) null));
    }

    @Test
    void whitelistedInternalAddressesAreAllowed() {
        var authorizer = UriAuthorizer.parse(" http://localhost:8080/schemas , https://10.0.0.5, http://[::1]:9000/ ");
        assertDoesNotThrow(() -> authorizer.check("http://localhost:8080/schemas/a.xsd"));
        assertDoesNotThrow(() -> authorizer.check("http://LOCALHOST:8080/schemas"));
        assertDoesNotThrow(() -> authorizer.check("https://10.0.0.5/anything/at/all"));
        assertDoesNotThrow(() -> authorizer.check("https://10.0.0.5:443/anything"));
        assertDoesNotThrow(() -> authorizer.check("http://[::1]:9000/x"));
        // Different path (also considering path segments), port, scheme or host.
        assertThrows(UriNotAllowedException.class, () -> authorizer.check("http://localhost:8080/schemas-private/a.xsd"));
        assertThrows(UriNotAllowedException.class, () -> authorizer.check("http://localhost:8080/other"));
        assertThrows(UriNotAllowedException.class, () -> authorizer.check("http://localhost:8081/schemas/a.xsd"));
        assertThrows(UriNotAllowedException.class, () -> authorizer.check("https://localhost:8080/schemas/a.xsd"));
        assertThrows(UriNotAllowedException.class, () -> authorizer.check("http://10.0.0.5/anything"));
        assertThrows(UriNotAllowedException.class, () -> authorizer.check("https://10.0.0.6/anything"));
    }

    @Test
    void pathTraversalDoesNotEscapeWhitelistedPath() {
        var authorizer = UriAuthorizer.parse("http://localhost:8080/schemas/");
        assertThrows(UriNotAllowedException.class, () -> authorizer.check("http://localhost:8080/schemas/../secret"));
    }

    @Test
    void whitelistDoesNotAffectNonHttpSchemes() {
        var authorizer = UriAuthorizer.parse("http://localhost:8080/");
        assertThrows(UriNotAllowedException.class, () -> authorizer.check("file://localhost:8080/etc/passwd"));
    }

    @Test
    void invalidWhitelistEntriesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> UriAuthorizer.parse("localhost:8080"));
        assertThrows(IllegalArgumentException.class, () -> UriAuthorizer.parse("file:///tmp"));
        assertThrows(IllegalArgumentException.class, () -> UriAuthorizer.parse("http://"));
        assertDoesNotThrow(() -> UriAuthorizer.parse(null));
        assertDoesNotThrow(() -> UriAuthorizer.parse(" , ,"));
    }

    @Test
    void isAllowedDoesNotThrow() {
        assertTrue(noWhitelist.isAllowed(URI.create("http://8.8.8.8/")));
        assertFalse(noWhitelist.isAllowed(URI.create("http://127.0.0.1/")));
    }

    @Test
    void messageDoesNotRevealResolvedAddresses() {
        var e = assertThrows(UriNotAllowedException.class, () -> noWhitelist.check("http://localhost/a"));
        assertFalse(e.getMessage().contains("127.0.0.1"));
    }

}
