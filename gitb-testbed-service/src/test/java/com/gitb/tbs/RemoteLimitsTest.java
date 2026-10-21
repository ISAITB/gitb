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

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.utils.RemoteResourceLimits;
import com.gitb.utils.UriAuthorizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.gitb.tbs.TdlTestHelper.configureOnly;
import static com.gitb.tbs.TdlTestHelper.run;
import static com.gitb.tbs.TdlTestHelper.runWithInteractions;
import static com.gitb.tbs.TdlTestHelper.stringInput;
import static com.gitb.tbs.TdlTestHelper.userInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the limits on the remote resources that a test step can load (number of requests, size of each resource,
 * total size and time per request).
 * <p>
 * Resources are served by the WireMock server below "/ext/", that is whitelisted. The requests received are counted to
 * verify that requests over the limit are not made. The limits are lowered for each test.
 */
class RemoteLimitsTest extends BaseIntegrationTest {

    private static final long KB = 1024;
    private static final String REQUESTS_EXCEEDED = "maximum allowed number of remote requests";

    @BeforeAll
    static void stubAll() {
        for (String id : new String[]{
                "sec-lim-assign", "sec-lim-verify", "sec-lim-process", "sec-lim-if", "sec-lim-while", "sec-lim-steps",
                "sec-lim-loop", "sec-lim-call", "sec-lim-send", "sec-lim-receive", "sec-lim-hidden", "sec-lim-output",
                "sec-lim-interact", "sec-lim-group", "sec-expr-unparsed-text", "sec-xsd", "sec-shacl", "sec-rdf-ask", "sec-uri-input"
        }) {
            stubTdl(id, "tdl/sec/" + id + ".xml");
        }
    }

    @BeforeEach
    void whitelistExternalResources() {
        UriAuthorizer.setGlobal(new UriAuthorizer(List.of(base() + "/ext/")));
    }

    @AfterEach
    void restoreDefaults() {
        UriAuthorizer.setGlobal(null);
        RemoteResourceLimits.setGlobal(null);
    }

    // Helpers - start.

    private String base() {
        return "http://localhost:" + wireMockPort();
    }

    private static void limits(long maxResourceSize, int maxRequests, long maxTotalSize) {
        RemoteResourceLimits.setGlobal(new RemoteResourceLimits(maxResourceSize, maxRequests, maxTotalSize, 30000));
    }

    private static void requestLimit(int maxRequests) {
        limits(10 * KB, maxRequests, 1024 * KB);
    }

    private static String content(int size) {
        return "x".repeat(size);
    }

    private String serve(String contentType, String body) {
        String path = "/ext/" + UUID.randomUUID();
        stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withHeader("Content-Type", contentType).withBody(body)));
        return path;
    }

    private String serveText(int size) {
        return serve("text/plain", content(size));
    }

    private static List<AnyContent> inputs(String... namesAndValues) {
        List<AnyContent> inputs = new ArrayList<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            inputs.add(stringInput(namesAndValues[i], namesAndValues[i + 1]));
        }
        return inputs;
    }

    private static String logs(TestRunResult result) {
        return String.join("\n", result.logMessages());
    }

    private static void assertFailedBecause(TestRunResult result, String expectedReason) {
        result.assertFailed();
        assertTrue(logs(result).contains(expectedReason), "Expected [" + expectedReason + "] in the logs:\n" + logs(result));
    }

    // Helpers - end.

    // Number of requests per kind of step - start.

    private void assertRequestsLimitedFor(String testCase, int lookups) throws Exception {
        String path = serveText(10);
        // Within the limit.
        requestLimit(lookups + 5);
        run(testCase, null, inputs("url", base() + path)).assertSuccess();
        assertEquals(lookups, requestCount(path));
        // Over the limit.
        String limitedPath = serveText(10);
        requestLimit(3);
        assertFailedBecause(run(testCase, null, inputs("url", base() + limitedPath)), REQUESTS_EXCEEDED + " (3)");
        assertEquals(3, requestCount(limitedPath), "Requests over the limit must not be made");
    }

    @Test
    void assignStepRequestsAreLimited() throws Exception {
        assertRequestsLimitedFor("sec-lim-assign", 5);
    }

    @Test
    void verifyStepRequestsAreLimited() throws Exception {
        assertRequestsLimitedFor("sec-lim-verify", 5);
    }

    @Test
    void processStepRequestsAreLimited() throws Exception {
        assertRequestsLimitedFor("sec-lim-process", 5);
    }

    @Test
    void ifStepConditionRequestsAreLimited() throws Exception {
        assertRequestsLimitedFor("sec-lim-if", 5);
    }

    @Test
    void callStepInputRequestsAreLimited() throws Exception {
        assertRequestsLimitedFor("sec-lim-call", 5);
    }

    @Test
    void sendStepInputRequestsAreLimited() throws Exception {
        assertRequestsLimitedFor("sec-lim-send", 5);
    }

    @Test
    void receiveStepInputRequestsAreLimited() throws Exception {
        assertRequestsLimitedFor("sec-lim-receive", 5);
    }

    @Test
    void interactionInstructionRequestsAreLimited() throws Exception {
        String path = serveText(10);
        requestLimit(10);
        runWithInteractions("sec-lim-interact", inputs("url", base() + path), request -> List.of(userInput(2, "value", "v"))).assertSuccess();
        assertEquals(5, requestCount(path));
        String limitedPath = serveText(10);
        requestLimit(3);
        assertFailedBecause(runWithInteractions("sec-lim-interact", inputs("url", base() + limitedPath), request -> List.of(userInput(2, "value", "v"))), REQUESTS_EXCEEDED + " (3)");
        assertEquals(3, requestCount(limitedPath));
    }

    // Number of requests per kind of step - end.

    // Scoping of the budget - start.

    @Test
    void loopConditionRequestsAccumulateForTheLoopStep() throws Exception {
        String path = serveText(10);
        requestLimit(2);
        assertFailedBecause(run("sec-lim-while", null, inputs("url", base() + path)), REQUESTS_EXCEEDED + " (2)");
        String otherPath = serveText(10);
        requestLimit(10);
        run("sec-lim-while", null, inputs("url", base() + otherPath)).assertSuccess();
        assertEquals(3, requestCount(otherPath));
    }

    @Test
    void eachStepHasItsOwnBudget() throws Exception {
        // Three steps with three requests each (nine in total), over a limit of four per step.
        String path = serveText(10);
        requestLimit(4);
        run("sec-lim-steps", null, inputs("url", base() + path)).assertSuccess();
        assertEquals(9, requestCount(path));
    }

    @Test
    void stepsNestedInGroupsAndConditionsHaveTheirOwnBudgets() throws Exception {
        // Six steps in nested groups and an if (18 requests in total), each with exactly three requests over a limit of three
        // per step. The budget of a step is not the accumulation of the budgets of the steps it contains.
        String path = serveText(10);
        requestLimit(3);
        run("sec-lim-group", null, inputs("url", base() + path)).assertSuccess();
        assertEquals(18, requestCount(path));
        // The limit still applies to each of the steps.
        String limitedPath = serveText(10);
        requestLimit(2);
        assertFailedBecause(run("sec-lim-group", null, inputs("url", base() + limitedPath)), REQUESTS_EXCEEDED + " (2)");
    }

    @Test
    void eachIterationOfALoopHasItsOwnBudgets() throws Exception {
        String path = serveText(10);
        requestLimit(4);
        run("sec-lim-loop", null, inputs("url", base() + path)).assertSuccess();
        assertEquals(9, requestCount(path));
    }

    @Test
    void outputMessagesHaveTheirOwnBudget() throws Exception {
        String path = serveText(10);
        requestLimit(10);
        run("sec-lim-output", null, inputs("url", base() + path)).assertSuccess();
        assertEquals(5, requestCount(path));
        // A limit that is exceeded when calculating the output message fails the session.
        String limitedPath = serveText(10);
        requestLimit(3);
        run("sec-lim-output", null, inputs("url", base() + limitedPath)).assertFailed();
        assertEquals(3, requestCount(limitedPath));
    }

    @Test
    void sessionConfigurationHasItsOwnBudget() throws Exception {
        // Inputs provided by reference are retrieved when configuring the session.
        String path = serveText(2 * (int) KB);
        limits(1 * KB, 10, 1024 * KB);
        var result = configureOnly("sec-uri-input", List.of(uriInput("data", base() + path)));
        assertNotNull(result.getErrorCode(), "The configuration should have failed");
        assertTrue(String.valueOf(result.getErrorDescription()).contains("exceeds the maximum allowed size of 1 KB"), result.getErrorDescription());
        limits(10 * KB, 10, 1024 * KB);
        assertNull(configureOnly("sec-uri-input", List.of(uriInput("data", base() + serve("text/plain", "retrieved")))).getErrorCode());
    }

    private static AnyContent uriInput(String name, String uri) {
        AnyContent input = stringInput(name, uri);
        input.setEmbeddingMethod(ValueEmbeddingEnumeration.URI);
        return input;
    }

    // Scoping of the budget - end.

    // Violations that could be hidden - start.

    @Test
    void aViolationHiddenBehindAnAvailabilityCheckStillFailsTheStep() throws Exception {
        String path = serveText(10);
        requestLimit(2);
        assertFailedBecause(run("sec-lim-hidden", null, inputs("url", base() + path)), REQUESTS_EXCEEDED + " (2)");
        String otherPath = serveText(10);
        requestLimit(10);
        run("sec-lim-hidden", null, inputs("url", base() + otherPath)).assertSuccess();
    }

    // Violations that could be hidden - end.

    // Sizes and time - start.

    @Test
    void sizeOfAResourceIsLimitedForExpressions() throws Exception {
        String path = serveText((int) (5 * KB));
        limits(1 * KB, 10, 1024 * KB);
        assertFailedBecause(run("sec-expr-unparsed-text", null, inputs("url", base() + path, "expected", "x")), "exceeds the maximum allowed size of 1 KB");
        String smallPath = serveText(10);
        run("sec-expr-unparsed-text", null, inputs("url", base() + smallPath, "expected", content(10))).assertSuccess();
    }

    @Test
    void totalSizeOfResourcesIsLimitedPerStep() throws Exception {
        String path = serveText((int) KB);
        // Five lookups of 1 KB each, over a total of 3 KB.
        limits(2 * KB, 10, 3 * KB);
        assertFailedBecause(run("sec-lim-assign", null, inputs("url", base() + path)), "exceed the maximum allowed total size of 3 KB");
    }

    @Test
    void stalledResponsesTimeOut() throws Exception {
        String path = "/ext/" + UUID.randomUUID();
        stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withBody(content(100)).withChunkedDribbleDelay(5, 8000)));
        RemoteResourceLimits.setGlobal(new RemoteResourceLimits(10 * KB, 10, 1024 * KB, 1000));
        long start = System.nanoTime();
        assertFailedBecause(run("sec-expr-unparsed-text", null, inputs("url", base() + path, "expected", "x")), "was not completed within the maximum allowed time");
        assertTrue((System.nanoTime() - start) / 1_000_000_000L < 7, "The lookup should have been interrupted by the timeout");
    }

    private static String turtle(int triples) {
        StringBuilder builder = new StringBuilder("@prefix ex: <http://example.org/> .\n");
        for (int i = 0; i < triples; i++) {
            builder.append("ex:s").append(i).append(" ex:p ex:o").append(i).append(" .\n");
        }
        return builder.toString();
    }

    @Test
    void sizeOfAnImportThatWouldBeIgnoredIfItFailedStillFailsTheStep() throws Exception {
        // Imports that cannot be read are normally ignored.
        String path = serve("text/turtle", turtle(500));
        String model = "@prefix owl: <http://www.w3.org/2002/07/owl#> . <http://example.org/onto> a owl:Ontology ; owl:imports <" + base() + path + "> .";
        String shapes = "@prefix sh: <http://www.w3.org/ns/shacl#> . @prefix ex: <http://example.org/> . ex:PersonShape a sh:NodeShape ; sh:targetClass ex:Person ; sh:property [ sh:path ex:name ; sh:minCount 1 ] .";
        List<AnyContent> inputs = inputs("model", model, "modelType", "text/turtle", "shapes", shapes, "shapesType", "text/turtle");
        AnyContent loadImports = stringInput("loadImports", "true");
        loadImports.setType("boolean");
        inputs.add(loadImports);
        limits(1 * KB, 10, 1024 * KB);
        assertFailedBecause(run("sec-shacl", null, inputs), "exceeds the maximum allowed size of 1 KB");
        limits(100 * KB, 10, 1024 * KB);
        run("sec-shacl", null, inputs).assertSuccess();
    }

    @Test
    void sizeOfAJsonLdContextIsLimited() throws Exception {
        StringBuilder context = new StringBuilder("{\"@context\":{\"@vocab\":\"http://example.org/\"");
        for (int i = 0; i < 300; i++) {
            context.append(",\"term").append(i).append("\":\"http://example.org/term").append(i).append('"');
        }
        String path = serve("application/ld+json", context.append("}}").toString());
        String model = "{\"@context\":\"" + base() + path + "\",\"@id\":\"http://example.org/alice\",\"@type\":\"Person\",\"name\":\"Alice\"}";
        String shapes = "@prefix sh: <http://www.w3.org/ns/shacl#> . @prefix ex: <http://example.org/> . ex:PersonShape a sh:NodeShape ; sh:targetClass ex:Person ; sh:property [ sh:path ex:name ; sh:minCount 1 ] .";
        List<AnyContent> inputs = inputs("model", model, "modelType", "application/ld+json", "shapes", shapes, "shapesType", "text/turtle");
        AnyContent loadImports = stringInput("loadImports", "false");
        loadImports.setType("boolean");
        inputs.add(loadImports);
        limits(1 * KB, 10, 1024 * KB);
        assertFailedBecause(run("sec-shacl", null, inputs), "exceeds the maximum allowed size of 1 KB");
        limits(100 * KB, 10, 1024 * KB);
        run("sec-shacl", null, inputs).assertSuccess();
    }

    @Test
    void sizeOfARemoteSchemaIsLimited() throws Exception {
        String schema = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"urn:test:root\" targetNamespace=\"urn:test:root\" elementFormDefault=\"qualified\"><!--" + content((int) (5 * KB)) + "--><xs:simpleType name=\"NameType\"><xs:restriction base=\"xs:string\"/></xs:simpleType></xs:schema>";
        String path = serve("application/xml", schema);
        String xsd = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"urn:test:root\" targetNamespace=\"urn:test:root\" elementFormDefault=\"qualified\"><xs:include schemaLocation=\"" + base() + path + "\"/>"
                + "<xs:element name=\"root\"><xs:complexType><xs:sequence><xs:element name=\"name\" type=\"NameType\"/></xs:sequence></xs:complexType></xs:element></xs:schema>";
        limits(1 * KB, 10, 1024 * KB);
        assertFailedBecause(run("sec-xsd", null, inputs("xsd", xsd)), "exceeds the maximum allowed size of 1 KB");
        limits(100 * KB, 10, 1024 * KB);
        run("sec-xsd", null, inputs("xsd", xsd)).assertSuccess();
    }

    @Test
    void sizeOfAServiceResponseIsLimited() throws Exception {
        StringBuilder bindings = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            bindings.append(i > 0 ? "," : "").append("{\"s\":{\"type\":\"uri\",\"value\":\"http://example.org/s").append(i).append("\"}}");
        }
        String path = serve("application/sparql-results+json", "{\"head\":{\"vars\":[\"s\"]},\"results\":{\"bindings\":[" + bindings + "]}}");
        String query = "ASK { SERVICE <" + base() + path + "> { ?s ?p ?o } }";
        limits(1 * KB, 10, 1024 * KB);
        assertFailedBecause(run("sec-rdf-ask", null, inputs("query", query)), "exceeds the maximum allowed size of 1 KB");
    }

    // Sizes and time - end.

}
