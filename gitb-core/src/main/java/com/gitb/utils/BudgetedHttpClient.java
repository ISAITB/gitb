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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * HTTP client that applies the {@link RemoteResourceBudget} bound to the calling thread (if any) to the requests made:
 * every request counts towards the maximum number of requests, and the bodies of responses (read as streams) are counted
 * and limited in size and time.
 * <p>
 * The budget is looked up when a request is made, on the thread making it. Only responses read as input streams are
 * limited: a response read by other means (e.g. as a string) is not.
 */
public class BudgetedHttpClient extends HttpClient {

    private final HttpClient delegate;

    /**
     * Constructor.
     *
     * @param delegate The client to delegate requests to.
     */
    public BudgetedHttpClient(HttpClient delegate) {
        this.delegate = delegate;
    }

    /**
     * Create a client that does not follow redirects, uses the default proxy selector and applies the budget.
     *
     * @param connectTimeout The connection timeout.
     * @return The client.
     */
    public static BudgetedHttpClient newClient(Duration connectTimeout) {
        return new BudgetedHttpClient(HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(Redirect.NEVER)
                .proxy(ProxySelector.getDefault())
                .build());
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return delegate.cookieHandler();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return delegate.connectTimeout();
    }

    @Override
    public Redirect followRedirects() {
        return delegate.followRedirects();
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return delegate.proxy();
    }

    @Override
    public SSLContext sslContext() {
        return delegate.sslContext();
    }

    @Override
    public SSLParameters sslParameters() {
        return delegate.sslParameters();
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return delegate.authenticator();
    }

    @Override
    public Version version() {
        return delegate.version();
    }

    @Override
    public Optional<Executor> executor() {
        return delegate.executor();
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        var budget = RemoteResourceBudget.current();
        if (budget == null) {
            return delegate.send(request, responseBodyHandler);
        }
        budget.onRequest(request.uri());
        return delegate.send(withTimeout(request, budget), limit(responseBodyHandler, budget, request));
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        var budget = RemoteResourceBudget.current();
        if (budget == null) {
            return delegate.sendAsync(request, responseBodyHandler);
        }
        budget.onRequest(request.uri());
        return delegate.sendAsync(withTimeout(request, budget), limit(responseBodyHandler, budget, request));
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        var budget = RemoteResourceBudget.current();
        if (budget == null) {
            return delegate.sendAsync(request, responseBodyHandler, pushPromiseHandler);
        }
        budget.onRequest(request.uri());
        return delegate.sendAsync(withTimeout(request, budget), limit(responseBodyHandler, budget, request), pushPromiseHandler);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public boolean awaitTermination(Duration duration) throws InterruptedException {
        return delegate.awaitTermination(duration);
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public void shutdownNow() {
        delegate.shutdownNow();
    }

    @Override
    public void close() {
        delegate.close();
    }

    private HttpRequest withTimeout(HttpRequest request, RemoteResourceBudget budget) {
        if (request.timeout().isPresent()) {
            return request;
        }
        // The timeout covers the time until the response headers are received. The reading of the body is limited separately.
        return HttpRequest.newBuilder(request, (name, value) -> true).timeout(budget.requestTimeout()).build();
    }

    @SuppressWarnings("unchecked")
    private <T> HttpResponse.BodyHandler<T> limit(HttpResponse.BodyHandler<T> handler, RemoteResourceBudget budget, HttpRequest request) {
        return info -> {
            long declaredLength = info.headers().firstValueAsLong("Content-Length").orElse(-1);
            return HttpResponse.BodySubscribers.mapping(handler.apply(info), body -> {
                if (body instanceof InputStream stream) {
                    return (T) budget.wrap(request.uri(), stream, declaredLength);
                }
                return body;
            });
        };
    }

}
