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

package com.gitb.engine.validation.handlers.pdf;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-scoped capture of the WARN (and above) log messages issued by PDFBox while parsing a
 * document, used to surface recoverable core-parsing issues that are only otherwise visible as log
 * output (e.g. xref rebuilds, bad object offsets).
 * <p>
 * PDFBox 3 exposes no public API to enumerate such recoverable issues or to switch parsing between a
 * strict and a lenient mode ({@code COSParser.setLenient} is {@code protected}), so this is the only
 * way to observe them. Capture is implemented by attaching a dedicated appender to the
 * {@code org.apache.pdfbox} Logback logger; messages are routed to a {@link ThreadLocal} list so that
 * concurrent sessions (each processed on their own thread) do not interleave.
 * <p>
 * If the runtime's SLF4J binding is not Logback, capture silently degrades to always returning an
 * empty message list - core-parse findings then only come from the explicit PDFBox API checks
 * performed elsewhere in {@link PdfValidator}.
 */
public class PdfBoxLogCapture implements AutoCloseable {

    private static final String PDFBOX_LOGGER_NAME = "org.apache.pdfbox";
    private static final ThreadLocal<List<String>> CAPTURED_MESSAGES = new ThreadLocal<>();
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private final boolean active;

    private PdfBoxLogCapture(boolean active) {
        this.active = active;
        if (active) {
            CAPTURED_MESSAGES.set(new ArrayList<>());
        }
    }

    /**
     * Start capturing PDFBox log messages on the current thread. The returned instance should be
     * closed (ideally via try-with-resources) once the parse it is covering has completed.
     *
     * @return The started capture.
     */
    public static PdfBoxLogCapture start() {
        return new PdfBoxLogCapture(ensureInstalled());
    }

    /**
     * @return The messages captured so far on the current thread, in the order they were logged
     * (empty if capture could not be installed).
     */
    public List<String> messages() {
        var messages = CAPTURED_MESSAGES.get();
        return messages == null ? Collections.emptyList() : List.copyOf(messages);
    }

    @Override
    public void close() {
        if (active) {
            CAPTURED_MESSAGES.remove();
        }
    }

    /**
     * Ensure the capturing appender is attached to the {@code org.apache.pdfbox} logger. Idempotent
     * and thread-safe - the attachment is only ever performed once, on the first call.
     *
     * @return Whether Logback is the active SLF4J binding (and hence capture is possible).
     */
    private static boolean ensureInstalled() {
        var slf4jLogger = LoggerFactory.getLogger(PDFBOX_LOGGER_NAME);
        if (!(slf4jLogger instanceof Logger pdfBoxLogger)) {
            // The runtime's SLF4J binding is not Logback - capture is not possible.
            return false;
        }
        if (INSTALLED.compareAndSet(false, true)) {
            // Raise the logger's own level to WARN (its effective level otherwise defaults to the
            // root logger's, which is ERROR), and disable additivity so that captured messages do not
            // also flow into the application's regular log appenders.
            pdfBoxLogger.setLevel(Level.WARN);
            pdfBoxLogger.setAdditive(false);
            var appender = new AppenderBase<ILoggingEvent>() {
                @Override
                protected void append(ILoggingEvent event) {
                    var messages = CAPTURED_MESSAGES.get();
                    if (messages != null) {
                        messages.add(event.getFormattedMessage());
                    }
                }
            };
            appender.setContext(pdfBoxLogger.getLoggerContext());
            appender.setName("PdfValidatorCapture");
            appender.start();
            pdfBoxLogger.addAppender(appender);
        }
        return true;
    }

}
