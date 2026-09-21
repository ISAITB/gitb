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

package com.gitb.engine.messaging.handlers.layer.application.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Minimal HTTP/1.1 connection wrapper around a raw {@link Socket}, replacing the low-level Apache HttpCore
 * connection classes ({@code DefaultBHttpClientConnection} / {@code DefaultBHttpServerConnection}) previously used
 * by {@link HttpSender} and {@link HttpReceiver}.
 * <p>
 * A single instance is shared, for the lifetime of a test session transaction, between whichever of
 * {@link HttpSender} and {@link HttpReceiver} acts first on the underlying socket. The {@link Role} recorded at
 * creation time determines which side of the exchange each side is expected to play from then on:
 * <ul>
 *     <li>{@link Role#CLIENT} - this side opened (or was handed) the socket in order to send a request first, and
 *     will subsequently read the response (mirrors the previous {@code DefaultBHttpClientConnection} usage).</li>
 *     <li>{@link Role#SERVER} - this side accepted an incoming socket in order to read a request first, and will
 *     subsequently write the response (mirrors the previous {@code DefaultBHttpServerConnection} usage).</li>
 * </ul>
 */
class SimpleHttpConnection {

    static final int BUFFER_SIZE = 8 * 1024;

    enum Role { CLIENT, SERVER }

    private final Role role;
    private final BufferedInputStream inputStream;
    private final BufferedOutputStream outputStream;

    SimpleHttpConnection(Socket socket, Role role) throws IOException {
        this.role = role;
        this.inputStream = new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE);
        this.outputStream = new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE);
    }

    Role role() {
        return role;
    }

    BufferedInputStream inputStream() {
        return inputStream;
    }

    BufferedOutputStream outputStream() {
        return outputStream;
    }

    void flush() throws IOException {
        outputStream.flush();
    }
}
