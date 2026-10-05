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

package com.gitb.tbs;

import com.gitb.engine.TestEngine;
import com.gitb.tbs.impl.TestbedServiceCallbackHandler;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public abstract class BaseIntegrationTest {

    private static final WireMockServer WIRE_MOCK;

    static {
        WIRE_MOCK = new WireMockServer(wireMockConfig().dynamicPort());
        WIRE_MOCK.start();
        int port = WIRE_MOCK.port();
        System.setProperty("remote.testcase.repository.url",
                "http://localhost:" + port + "/tests/:test_id/definition");
        System.setProperty("remote.testresource.repository.url",
                "http://localhost:" + port + "/resources/:test_id/:resource_id");
        System.setProperty("CALLBACK_ROOT_URL", "http://localhost:8080/");
        TestEngine.getInstance().initialize(TestbedServiceCallbackHandler.getInstance());
    }

    protected static int wireMockPort() {
        return WIRE_MOCK.port();
    }

    protected static StubMapping stubFor(MappingBuilder mappingBuilder) {
        return WIRE_MOCK.stubFor(mappingBuilder);
    }

    /**
     * Stub a test suite resource (artifact) lookup, as served by the test resource repository.
     *
     * @param pathRegex Regular expression matching the request path (the artifact's path is URL-encoded in it).
     * @param classpathResource The classpath resource to respond with.
     */
    protected static void stubTestResource(String pathRegex, String classpathResource) {
        try (InputStream is = BaseIntegrationTest.class.getClassLoader()
                .getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + classpathResource);
            }
            WIRE_MOCK.stubFor(get(urlPathMatching(pathRegex))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/xml; charset=UTF-8")
                            .withBody(new String(is.readAllBytes(), StandardCharsets.UTF_8))));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource: " + classpathResource, e);
        }
    }

    /**
     * Count the requests received for the provided path (to check whether a call was made).
     *
     * @param path The exact request path.
     * @return The number of requests received.
     */
    protected static int requestCount(String path) {
        return WIRE_MOCK.countRequestsMatching(getRequestedFor(urlPathEqualTo(path)).build()).getCount();
    }

    protected static void stubTdl(String testCaseId, String classpathResource) {
        try (InputStream is = BaseIntegrationTest.class.getClassLoader()
                .getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + classpathResource);
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            WIRE_MOCK.stubFor(get(urlPathEqualTo("/tests/" + testCaseId + "/definition"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/xml; charset=UTF-8")
                            .withBody(content)));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read TDL resource: " + classpathResource, e);
        }
    }
}
