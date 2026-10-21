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

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class GuardedUriReaderTest {

    private static WireMockServer server;
    private static String base;
    // Only /ok/ is whitelisted - everything else on this (internal) server is not.
    private static UriAuthorizer authorizer;

    @BeforeAll
    static void start() {
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        base = "http://localhost:" + server.port();
        authorizer = new UriAuthorizer(List.of(base + "/ok/"));
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    @BeforeEach
    void reset() {
        server.resetAll();
    }

    private String read(String path) throws Exception {
        try (var response = GuardedUriReader.open(URI.create(base + path), "application/json", authorizer)) {
            return new String(response.stream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void allowedUriIsRead() throws Exception {
        server.stubFor(get("/ok/doc").willReturn(aResponse().withHeader("Content-Type", "application/json; charset=UTF-8").withBody("{}")));
        try (var response = GuardedUriReader.open(URI.create(base + "/ok/doc"), "application/json", authorizer)) {
            assertEquals("{}", new String(response.stream().readAllBytes(), StandardCharsets.UTF_8));
            assertEquals("application/json", response.contentType().orElseThrow());
        }
        server.verify(getRequestedFor(urlEqualTo("/ok/doc")).withHeader("Accept", equalTo("application/json")));
    }

    @Test
    void notAllowedUriIsNotRequested() {
        server.stubFor(get("/internal/doc").willReturn(aResponse().withBody("secret")));
        assertThrows(UriNotAllowedException.class, () -> read("/internal/doc"));
        server.verify(0, getRequestedFor(urlEqualTo("/internal/doc")));
    }

    @Test
    void redirectToAllowedUriIsFollowed() throws Exception {
        server.stubFor(get("/ok/redirect").willReturn(aResponse().withStatus(302).withHeader("Location", "/ok/final")));
        server.stubFor(get("/ok/final").willReturn(aResponse().withBody("final")));
        assertEquals("final", read("/ok/redirect"));
    }

    @Test
    void redirectToNotAllowedUriIsBlocked() {
        server.stubFor(get("/ok/redirect").willReturn(aResponse().withStatus(307).withHeader("Location", base + "/internal/secret")));
        server.stubFor(get("/internal/secret").willReturn(aResponse().withBody("secret")));
        assertThrows(UriNotAllowedException.class, () -> read("/ok/redirect"));
        server.verify(0, getRequestedFor(urlEqualTo("/internal/secret")));
    }

    @Test
    void redirectToOtherSchemeIsBlocked() {
        server.stubFor(get("/ok/redirect").willReturn(aResponse().withStatus(302).withHeader("Location", "file:///etc/passwd")));
        assertThrows(UriNotAllowedException.class, () -> read("/ok/redirect"));
    }

    @Test
    void redirectLoopIsDetected() {
        server.stubFor(get("/ok/a").willReturn(aResponse().withStatus(302).withHeader("Location", "/ok/b")));
        server.stubFor(get("/ok/b").willReturn(aResponse().withStatus(302).withHeader("Location", "/ok/a")));
        var e = assertThrows(RuntimeException.class, () -> read("/ok/a"));
        assertTrue(e.getMessage().contains("Redirect loop"), e.getMessage());
    }

    @Test
    void tooManyRedirectsFail() {
        for (int i = 0; i <= GuardedUriReader.MAX_REDIRECTS + 2; i++) {
            server.stubFor(get("/ok/hop" + i).willReturn(aResponse().withStatus(302).withHeader("Location", "/ok/hop" + (i + 1))));
        }
        var e = assertThrows(RuntimeException.class, () -> read("/ok/hop0"));
        assertTrue(e.getMessage().contains("Too many redirects"), e.getMessage());
    }

    @Test
    void redirectWithoutLocationFails() {
        server.stubFor(get("/ok/nolocation").willReturn(aResponse().withStatus(302)));
        assertThrows(RuntimeException.class, () -> read("/ok/nolocation"));
    }

    @Test
    void errorStatusFails() {
        server.stubFor(get("/ok/missing").willReturn(aResponse().withStatus(404)));
        var e = assertThrows(RuntimeException.class, () -> read("/ok/missing"));
        assertTrue(e.getMessage().contains("404"), e.getMessage());
    }

}
