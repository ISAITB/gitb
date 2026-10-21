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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Limits the remote resources loaded while a unit of work (typically a test step) executes: the number of requests
 * (including redirects), the size of each resource, the total size of all resources, and the time allowed for each request.
 * <p>
 * A budget is bound to the thread doing the work (see {@link #bind()}) and is applied by the HTTP clients used to load
 * remote resources (see {@link BudgetedHttpClient}). The work of a test step is made up of units (actor messages and
 * futures) that execute on different threads, one after the other. The same budget can therefore be bound for each unit,
 * so that it accumulates across all the work of the step.
 * <p>
 * Once a limit is exceeded the budget is exhausted: every further request fails and the violation is remembered, to be
 * raised when the work completes (see {@link #call(Callable)}). This ensures that a violation is not hidden by code that
 * wraps or ignores the failure.
 */
public final class RemoteResourceBudget {

    private static final ThreadLocal<RemoteResourceBudget> CURRENT = new ThreadLocal<>();
    private static final ScheduledThreadPoolExecutor WATCHDOG = newWatchdog();

    private final RemoteResourceLimits limits;
    private int requests = 0;
    private long totalSize = 0;
    private RemoteResourceLimitException violation;

    private RemoteResourceBudget(RemoteResourceLimits limits) {
        this.limits = limits;
    }

    /**
     * An open scope, to be closed once the work it covers is complete.
     */
    public interface Scope extends AutoCloseable {

        /**
         * Close the scope, restoring the state before it was opened.
         */
        @Override
        void close();

    }

    private static ScheduledThreadPoolExecutor newWatchdog() {
        var executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            var thread = new Thread(runnable, "remote-resource-watchdog");
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    /**
     * Create a budget, applying the limits configured for the engine at this time.
     *
     * @return The budget.
     */
    public static RemoteResourceBudget create() {
        return new RemoteResourceBudget(RemoteResourceLimits.global());
    }

    /**
     * Create a budget with specific limits.
     *
     * @param limits The limits.
     * @return The budget.
     */
    public static RemoteResourceBudget create(RemoteResourceLimits limits) {
        return new RemoteResourceBudget(limits);
    }

    /**
     * @return The budget bound to the current thread (null if the lookups of the thread are not limited).
     */
    public static RemoteResourceBudget current() {
        return CURRENT.get();
    }

    /**
     * Bind this budget to the current thread. Closing the returned scope restores the budget that was bound before.
     *
     * @return The scope to close once the work is complete.
     */
    public Scope bind() {
        var previous = CURRENT.get();
        CURRENT.set(this);
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    /**
     * Do work with this budget bound to the current thread. If a limit was exceeded this is the reason for the work to
     * fail, whether the work failed (possibly with a failure that wraps the violation) or completed.
     *
     * @param work The work.
     * @return The result of the work.
     * @param <T> The result type.
     * @throws RemoteResourceLimitException If a limit was exceeded.
     * @throws Exception If the work failed.
     */
    public <T> T call(Callable<T> work) throws Exception {
        try (var ignored = bind()) {
            T result;
            try {
                result = work.call();
            } catch (Exception e) {
                raiseIfExceeded();
                throw e;
            }
            raiseIfExceeded();
            return result;
        }
    }

    /**
     * Do work with this budget bound to the current thread (see {@link #call(Callable)}).
     *
     * @param work The work.
     * @throws RemoteResourceLimitException If a limit was exceeded.
     * @throws Exception If the work failed.
     */
    public void run(ThrowingRunnable work) throws Exception {
        call(() -> {
            work.run();
            return null;
        });
    }

    /**
     * Do work, that is not part of a test step, with a new budget.
     *
     * @param work The work.
     * @return The result.
     * @param <T> The result type.
     * @throws RemoteResourceLimitException If a limit was exceeded.
     */
    public static <T> T callScoped(Supplier<T> work) {
        var budget = create();
        try (var ignored = budget.bind()) {
            T result;
            try {
                result = work.get();
            } catch (RuntimeException e) {
                budget.raiseIfExceeded();
                throw e;
            }
            budget.raiseIfExceeded();
            return result;
        }
    }

    /**
     * Do work, that is not part of a test step, with a new budget.
     *
     * @param work The work.
     * @throws RemoteResourceLimitException If a limit was exceeded.
     */
    public static void runScoped(Runnable work) {
        callScoped(() -> {
            work.run();
            return null;
        });
    }

    /**
     * Raise the reason why the budget was exhausted, if this is the case.
     *
     * @throws RemoteResourceLimitException If a limit was exceeded.
     */
    public void raiseIfExceeded() {
        synchronized (this) {
            if (violation != null) {
                throw violation;
            }
        }
    }

    /**
     * Record a remote request, which includes those made to follow redirects.
     *
     * @param uri The URI of the request.
     * @throws RemoteResourceLimitException If a limit has been exceeded.
     */
    public synchronized void onRequest(URI uri) {
        if (violation != null) {
            throw violation;
        }
        requests += 1;
        if (requests > limits.maxRequests()) {
            throw fail("The test step required more than the maximum allowed number of remote requests (%s).".formatted(limits.maxRequests()));
        }
    }

    /**
     * @return The maximum time allowed for a request.
     */
    public Duration requestTimeout() {
        return Duration.ofMillis(limits.requestTimeoutMillis());
    }

    /**
     * Wrap the body of a response so that its reading counts towards the budget and is limited accordingly.
     *
     * @param uri The URI of the request.
     * @param body The response body.
     * @param declaredLength The length declared by the response (negative if unknown). A declared length that exceeds
     *                       a limit is rejected before any of the body is read.
     * @return The stream to use in place of the body.
     */
    public InputStream wrap(URI uri, InputStream body, long declaredLength) {
        return new LimitedStream(uri, body, declaredLength);
    }

    private synchronized RemoteResourceLimitException fail(String message) {
        if (violation == null) {
            violation = new RemoteResourceLimitException(message);
        }
        return violation;
    }

    private synchronized void addSize(long bytes) {
        totalSize += bytes;
    }

    private synchronized long currentTotalSize() {
        return totalSize;
    }

    private synchronized boolean isTotalSizeExceeded() {
        return totalSize > limits.maxTotalSize();
    }

    private static String displaySize(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L && bytes % (1024L * 1024L * 1024L) == 0) {
            return (bytes / (1024L * 1024L * 1024L)) + " GB";
        } else if (bytes >= 1024L * 1024L && bytes % (1024L * 1024L) == 0) {
            return (bytes / (1024L * 1024L)) + " MB";
        } else if (bytes >= 1024L && bytes % 1024L == 0) {
            return (bytes / 1024L) + " KB";
        }
        return bytes + " bytes";
    }

    private String resourceTooLargeMessage(URI uri) {
        return "The remote resource [%s] exceeds the maximum allowed size of %s.".formatted(uri, displaySize(limits.maxResourceSize()));
    }

    private String totalTooLargeMessage() {
        return "The remote resources loaded by the test step exceed the maximum allowed total size of %s.".formatted(displaySize(limits.maxTotalSize()));
    }

    /**
     * Stream that counts and limits the bytes read from a response.
     */
    private final class LimitedStream extends FilterInputStream {

        private final URI uri;
        private final long deadline;
        private final ScheduledFuture<?> watchdog;
        private final long declaredLength;
        private boolean declaredLengthChecked = false;
        private long size = 0;
        private volatile boolean timedOut = false;

        private LimitedStream(URI uri, InputStream body, long declaredLength) {
            super(body);
            this.uri = uri;
            this.declaredLength = declaredLength;
            this.deadline = System.nanoTime() + requestTimeout().toNanos();
            // The timeout also needs to apply to reads that block, which can only be done by closing the stream.
            this.watchdog = WATCHDOG.schedule(this::expire, requestTimeout().toNanos(), TimeUnit.NANOSECONDS);
        }

        private void expire() {
            timedOut = true;
            fail("The request for remote resource [%s] was not completed within the maximum allowed time of %s seconds.".formatted(uri, requestTimeout().toMillis() / 1000.0));
            closeQuietly();
        }

        private void closeQuietly() {
            watchdog.cancel(false);
            try {
                in.close();
            } catch (IOException e) {
                // Ignore: we are failing already.
            }
        }

        private void checkAllowed() {
            synchronized (RemoteResourceBudget.this) {
                if (violation != null) {
                    closeQuietly();
                    throw violation;
                }
            }
            if (System.nanoTime() - deadline > 0) {
                expire();
                throw violation;
            }
            if (!declaredLengthChecked) {
                declaredLengthChecked = true;
                if (declaredLength > limits.maxResourceSize()) {
                    closeQuietly();
                    throw fail(resourceTooLargeMessage(uri));
                }
                if (declaredLength > 0 && declaredLength > limits.maxTotalSize() - currentTotalSize()) {
                    closeQuietly();
                    throw fail(totalTooLargeMessage());
                }
            }
        }

        private void account(long bytes) {
            size += bytes;
            addSize(bytes);
            if (size > limits.maxResourceSize()) {
                closeQuietly();
                throw fail(resourceTooLargeMessage(uri));
            }
            if (isTotalSizeExceeded()) {
                closeQuietly();
                throw fail(totalTooLargeMessage());
            }
        }

        private IOException translate(IOException e) {
            if (timedOut) {
                throw violation;
            }
            return e;
        }

        @Override
        public int read() throws IOException {
            checkAllowed();
            try {
                int value = in.read();
                if (value < 0) {
                    watchdog.cancel(false);
                } else {
                    account(1);
                }
                return value;
            } catch (IOException e) {
                throw translate(e);
            }
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            checkAllowed();
            try {
                int count = in.read(buffer, offset, length);
                if (count < 0) {
                    watchdog.cancel(false);
                } else {
                    account(count);
                }
                return count;
            } catch (IOException e) {
                throw translate(e);
            }
        }

        @Override
        public long skip(long n) throws IOException {
            // Skipped bytes are also received, so they are read to be counted.
            byte[] discard = new byte[(int) Math.clamp(n, 0, 8192)];
            long skipped = 0;
            while (skipped < n) {
                int count = read(discard, 0, (int) Math.min(discard.length, n - skipped));
                if (count < 0) {
                    break;
                }
                skipped += count;
            }
            return skipped;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public void close() throws IOException {
            watchdog.cancel(false);
            super.close();
        }

    }

}
