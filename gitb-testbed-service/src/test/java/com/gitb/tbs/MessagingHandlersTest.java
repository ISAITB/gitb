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
import com.gitb.messaging.MessagingReport;
import com.gitb.utils.MessagingReportUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.gitb.tbs.TdlTestHelper.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

class MessagingHandlersTest extends BaseIntegrationTest {

    private static final String API_KEY = "test-api-key";

    @BeforeAll
    static void stubAll() {
        for (String id : new String[]{
                "msg-simulated-send-success", "msg-simulated-send-failure",
                "msg-simulated-send-report-items", "msg-simulated-send-output",
                "msg-simulated-receive-immediate", "msg-simulated-receive-deferred",
                "msg-simulated-receive-timeout", "msg-simulated-send-receive",
                "msg-simulated-explicit-txn", "msg-simulated-send-invert",
                "msg-http-send-get", "msg-http-send-post", "msg-http-send-query-params",
                "msg-http-receive-basic", "msg-http-receive-status", "msg-http-send-receive",
                "msg-soap-send-basic", "msg-soap-send-action", "msg-soap-send-tolerate",
                "msg-soap-receive-basic",
                "msg-domibus-receive-ack", "msg-domibus-receive-expiry"
        }) {
            stubTdl(id, "tdl/msg/" + id + ".xml");
        }

        // HTTP send stubs
        stubFor(get(urlPathEqualTo("/http-get-target"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(post(urlPathEqualTo("/http-post-target"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(get(urlPathEqualTo("/http-query-target"))
                .willReturn(aResponse().withStatus(200)));

        // SOAP send stubs
        String soap11Response =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soapenv:Body><response>OK</response></soapenv:Body></soapenv:Envelope>";
        stubFor(post(urlPathEqualTo("/soap-target"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml; charset=UTF-8")
                        .withBody(soap11Response)));
        stubFor(post(urlPathEqualTo("/soap-non-soap-response"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("OK")));

        // Domibus acknowledgement stub: getStatus returns ACKNOWLEDGED
        stubFor(post(urlMatching(".*"))
                .withRequestBody(containing("statusRequest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/soap+xml; charset=UTF-8")
                        .withBody("<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" " +
                                "xmlns:tns=\"http://org.ecodex.backend/1_1/\">" +
                                "<soap:Body><tns:getStatusResponse>ACKNOWLEDGED</tns:getStatusResponse>" +
                                "</soap:Body></soap:Envelope>")));

        // Domibus expiry stub: listPendingMessages returns empty list
        stubFor(post(urlMatching(".*"))
                .withRequestBody(containing("listPendingMessages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/soap+xml; charset=UTF-8")
                        .withBody("<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" " +
                                "xmlns:tns=\"http://org.ecodex.backend/1_1/\">" +
                                "<soap:Body><tns:listPendingMessagesResponse/>" +
                                "</soap:Body></soap:Envelope>")));
    }

    private List<AnyContent> backendUrlInput() {
        return List.of(stringInput("backendUrl", "http://localhost:" + wireMockPort()));
    }

    private static MessagingReport successReport() {
        return MessagingReportUtils.generateSuccessReport(null);
    }

    // SimulatedMessaging tests

    @Test
    void simulatedSendSuccess() throws Exception {
        assertSuccess(run("msg-simulated-send-success"));
    }

    @Test
    void simulatedSendFailure() throws Exception {
        assertFailed(run("msg-simulated-send-failure"));
    }

    @Test
    void simulatedSendReportItems() throws Exception {
        assertSuccess(run("msg-simulated-send-report-items"));
    }

    @Test
    void simulatedSendOutputCaptured() throws Exception {
        assertSuccess(run("msg-simulated-send-output"));
    }

    @Test
    void simulatedReceiveImmediate() throws Exception {
        assertSuccess(run("msg-simulated-receive-immediate"));
    }

    @Test
    void simulatedReceiveDeferred() throws Exception {
        assertSuccess(run("msg-simulated-receive-deferred"));
    }

    @Test
    void simulatedReceiveTimeout() throws Exception {
        assertFailed(run("msg-simulated-receive-timeout"));
    }

    @Test
    void simulatedSendThenReceive() throws Exception {
        assertSuccess(run("msg-simulated-send-receive"));
    }

    @Test
    void simulatedExplicitTransaction() throws Exception {
        assertSuccess(run("msg-simulated-explicit-txn"));
    }

    @Test
    void simulatedSendInvert() throws Exception {
        assertSuccess(run("msg-simulated-send-invert"));
    }

    // HttpMessagingV2 tests

    @Test
    void httpSendGet() throws Exception {
        assertSuccess(run("msg-http-send-get", API_KEY, backendUrlInput()));
    }

    @Test
    void httpSendPost() throws Exception {
        assertSuccess(run("msg-http-send-post", API_KEY, backendUrlInput()));
    }

    @Test
    void httpSendQueryParams() throws Exception {
        assertSuccess(run("msg-http-send-query-params", API_KEY, backendUrlInput()));
    }

    @Test
    void httpReceiveBasic() throws Exception {
        assertSuccess(runWithCallback("msg-http-receive-basic", API_KEY, backendUrlInput(), 100, successReport()));
    }

    @Test
    void httpReceiveWithStatus() throws Exception {
        assertSuccess(runWithCallback("msg-http-receive-status", API_KEY, backendUrlInput(), 100, successReport()));
    }

    @Test
    void httpSendThenReceive() throws Exception {
        assertSuccess(runWithCallback("msg-http-send-receive", API_KEY, backendUrlInput(), 200, successReport()));
    }

    // SoapMessagingV2 tests

    @Test
    void soapSendBasic() throws Exception {
        assertSuccess(run("msg-soap-send-basic", API_KEY, backendUrlInput()));
    }

    @Test
    void soapSendWithAction() throws Exception {
        assertSuccess(run("msg-soap-send-action", API_KEY, backendUrlInput()));
    }

    @Test
    void soapSendTolerant() throws Exception {
        assertSuccess(run("msg-soap-send-tolerate", API_KEY, backendUrlInput()));
    }

    @Test
    void soapReceiveBasic() throws Exception {
        assertSuccess(runWithCallback("msg-soap-receive-basic", API_KEY, backendUrlInput(), 100, successReport()));
    }

    // DomibusMessaging tests

    @Test
    void domibusReceiveAck() throws Exception {
        assertSuccess(run("msg-domibus-receive-ack", API_KEY, backendUrlInput()));
    }

    @Test
    void domibusReceiveExpiry() throws Exception {
        assertFailed(run("msg-domibus-receive-expiry", API_KEY, backendUrlInput()));
    }
}
