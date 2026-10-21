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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the limits applied to the remote resources loaded while a test step executes.
 */
class RemoteResourceBudgetTest {

    private static final int KB = 1024;

    private final BudgetedHttpClient client = BudgetedHttpClient.newClient(Duration.ofSeconds(5));
    private HttpServer server;
    private final AtomicInteger requestsReceived = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/small", exchange -> respond(exchange, 200, 10 * KB, null));
        server.createContext("/medium", exchange -> respond(exchange, 200, 600 * KB, null));
        server.createContext("/large", exchange -> respond(exchange, 200, 2048 * KB, null));
        server.createContext("/chunked-large", exchange -> {
            requestsReceived.incrementAndGet();
            // No declared length, so the size can only be determined while reading.
            exchange.sendResponseHeaders(200, 0);
            byte[] chunk = new byte[64 * KB];
            try (var out = exchange.getResponseBody()) {
                for (int i = 0; i < 32; i++) {
                    out.write(chunk);
                }
            } catch (IOException e) {
                // The client stopped reading, as expected.
            }
        });
        server.createContext("/redirect/1", exchange -> respond(exchange, 302, 0, "/redirect/2"));
        server.createContext("/redirect/2", exchange -> respond(exchange, 302, 0, "/redirect/3"));
        server.createContext("/redirect/3", exchange -> respond(exchange, 200, 10 * KB, null));
        server.createContext("/stalled", exchange -> {
            requestsReceived.incrementAndGet();
            // Send part of the response and then stop.
            exchange.sendResponseHeaders(200, 100 * KB);
            exchange.getResponseBody().write(new byte[KB]);
            exchange.getResponseBody().flush();
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        UriAuthorizer.setGlobal(null);
    }

    private void respond(HttpExchange exchange, int status, int size, String location) throws IOException {
        requestsReceived.incrementAndGet();
        if (location != null) {
            exchange.getResponseHeaders().add("Location", location);
        }
        exchange.sendResponseHeaders(status, size == 0 ? -1 : size);
        if (size > 0) {
            byte[] chunk = new byte[Math.min(size, 64 * KB)];
            for (int written = 0; written < size; written += chunk.length) {
                exchange.getResponseBody().write(chunk, 0, Math.min(chunk.length, size - written));
            }
        }
        exchange.close();
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private static RemoteResourceLimits limits(int maxResourceKb, int maxRequests, int maxTotalKb) {
        return new RemoteResourceLimits(maxResourceKb * 1024L, maxRequests, maxTotalKb * 1024L, 30000);
    }

    private long readAll(String path) throws Exception {
        var response = client.send(HttpRequest.newBuilder(url(path)).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        try (var stream = response.body()) {
            return stream.readAllBytes().length;
        }
    }

    @Test
    void noLimitsWithoutABoundBudget() throws Exception {
        assertNull(RemoteResourceBudget.current());
        assertEquals(2048 * KB, readAll("/large"));
    }

    @Test
    void resourcesWithinTheLimitsAreLoaded() throws Exception {
        try (var ignored = RemoteResourceBudget.create(limits(1024, 10, 2048)).bind()) {
            assertEquals(10 * KB, readAll("/small"));
            assertEquals(600 * KB, readAll("/medium"));
        }
    }

    @Test
    void declaredSizeAboveTheResourceLimitIsRejected() {
        var budget = RemoteResourceBudget.create(limits(1024, 10, 8192));
        try (var ignored = budget.bind()) {
            var error = assertThrows(RemoteResourceLimitException.class, () -> readAll("/large"));
            assertTrue(error.getMessage().contains("exceeds the maximum allowed size of 1 MB"), error.getMessage());
        }
        assertThrows(RemoteResourceLimitException.class, budget::raiseIfExceeded);
    }

    @Test
    void undeclaredSizeAboveTheResourceLimitIsRejectedWhileReading() {
        try (var ignored = RemoteResourceBudget.create(limits(512, 10, 8192)).bind()) {
            var error = assertThrows(RemoteResourceLimitException.class, () -> readAll("/chunked-large"));
            assertTrue(error.getMessage().contains("exceeds the maximum allowed size of 512 KB"), error.getMessage());
        }
    }

    @Test
    void totalSizeAcrossResourcesIsLimited() throws Exception {
        try (var ignored = RemoteResourceBudget.create(limits(1024, 10, 1000)).bind()) {
            assertEquals(600 * KB, readAll("/medium"));
            var error = assertThrows(RemoteResourceLimitException.class, () -> readAll("/medium"));
            assertTrue(error.getMessage().contains("maximum allowed total size"), error.getMessage());
        }
    }

    @Test
    void numberOfRequestsIsLimitedAndTheBudgetStaysExhausted() throws Exception {
        var budget = RemoteResourceBudget.create(limits(1024, 2, 8192));
        try (var ignored = budget.bind()) {
            readAll("/small");
            readAll("/small");
            var error = assertThrows(RemoteResourceLimitException.class, () -> readAll("/small"));
            assertTrue(error.getMessage().contains("maximum allowed number of remote requests (2)"), error.getMessage());
            // Once exhausted, no further request is made.
            int received = requestsReceived.get();
            assertThrows(RemoteResourceLimitException.class, () -> readAll("/small"));
            assertEquals(received, requestsReceived.get());
        }
        assertThrows(RemoteResourceLimitException.class, budget::raiseIfExceeded);
    }

    @Test
    void redirectsCountAsRequests() {
        UriAuthorizer.setGlobal(new UriAuthorizer(List.of("http://127.0.0.1:" + server.getAddress().getPort() + "/")));
        // The redirect chain needs three requests.
        try (var ignored = RemoteResourceBudget.create(limits(1024, 2, 8192)).bind()) {
            assertThrows(RemoteResourceLimitException.class, () -> GuardedUriReader.open(url("/redirect/1"), null, UriAuthorizer.global()).stream().readAllBytes());
        }
        try (var ignored = RemoteResourceBudget.create(limits(1024, 3, 8192)).bind()) {
            assertDoesNotThrow(() -> GuardedUriReader.open(url("/redirect/1"), null, UriAuthorizer.global()).stream().readAllBytes());
        }
    }

    @Test
    void stalledResponseTimesOut() {
        var limits = new RemoteResourceLimits(1024L * KB, 10, 8192L * KB, 1000);
        long start = System.nanoTime();
        try (var ignored = RemoteResourceBudget.create(limits).bind()) {
            var error = assertThrows(RemoteResourceLimitException.class, () -> readAll("/stalled"));
            assertTrue(error.getMessage().contains("was not completed within the maximum allowed time"), error.getMessage());
        }
        assertTrue(Duration.ofNanos(System.nanoTime() - start).toSeconds() < 10, "The read should have been interrupted by the timeout");
    }

    @Test
    void responsesNotReadAsStreamsAreNotWrapped() throws Exception {
        try (var ignored = RemoteResourceBudget.create(limits(1, 10, 8192)).bind()) {
            var response = client.send(HttpRequest.newBuilder(url("/medium")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(600 * KB, response.body().length());
        }
    }

    @Test
    void bindingRestoresThePreviousBudget() {
        var outer = RemoteResourceBudget.create(limits(1024, 10, 8192));
        var inner = RemoteResourceBudget.create(limits(1024, 10, 8192));
        assertNull(RemoteResourceBudget.current());
        try (var ignored = outer.bind()) {
            assertSame(outer, RemoteResourceBudget.current());
            try (var ignoredInner = inner.bind()) {
                assertSame(inner, RemoteResourceBudget.current());
            }
            assertSame(outer, RemoteResourceBudget.current());
        }
        assertNull(RemoteResourceBudget.current());
    }

    @Test
    void theSameBudgetAccumulatesAcrossUnitsOfWork() throws Exception {
        // A step's work is made up of units that run one after the other (possibly on different threads), sharing the budget.
        var budget = RemoteResourceBudget.create(limits(1024, 3, 8192));
        budget.run(() -> readAll("/small"));
        var otherThread = new Thread(() -> {
            try {
                budget.run(() -> {
                    readAll("/small");
                    readAll("/small");
                });
            } catch (Exception e) {
                fail(e);
            }
        });
        otherThread.start();
        otherThread.join();
        var error = assertThrows(RemoteResourceLimitException.class, () -> budget.run(() -> readAll("/small")));
        assertTrue(error.getMessage().contains("number of remote requests (3)"), error.getMessage());
    }

    @Test
    void aViolationIsRaisedWhenTheWorkCompletesEvenIfTheWorkIgnoredIt() {
        var budget = RemoteResourceBudget.create(limits(1024, 10, 8192));
        var error = assertThrows(RemoteResourceLimitException.class, () -> budget.run(() -> {
            try {
                readAll("/large");
            } catch (RemoteResourceLimitException e) {
                // Ignored by the work, as a library might do (e.g. a lookup that is only checking for availability).
            }
        }));
        assertTrue(error.getMessage().contains("exceeds the maximum allowed size"), error.getMessage());
    }

    @Test
    void aViolationReplacesAFailureThatWrappedIt() {
        var budget = RemoteResourceBudget.create(limits(1024, 10, 8192));
        var error = assertThrows(RemoteResourceLimitException.class, () -> budget.run(() -> {
            try {
                readAll("/large");
            } catch (RemoteResourceLimitException e) {
                throw new IllegalStateException("Unable to load the schema", e);
            }
        }));
        assertTrue(error.getMessage().contains("exceeds the maximum allowed size"), error.getMessage());
    }

    @Test
    void workThatFailsForAnotherReasonKeepsItsFailure() {
        var budget = RemoteResourceBudget.create(limits(1024, 10, 8192));
        assertThrows(IllegalStateException.class, () -> budget.run(() -> {
            throw new IllegalStateException("Other failure");
        }));
        assertNull(RemoteResourceBudget.current());
    }

    @Test
    void scopedWorkWithoutAStepGetsItsOwnBudget() {
        var seen = new RemoteResourceBudget[2];
        RemoteResourceBudget.runScoped(() -> seen[0] = RemoteResourceBudget.current());
        RemoteResourceBudget.runScoped(() -> seen[1] = RemoteResourceBudget.current());
        assertNotNull(seen[0]);
        assertNotNull(seen[1]);
        assertNotSame(seen[0], seen[1]);
        assertNull(RemoteResourceBudget.current());
    }

}
