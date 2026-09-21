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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal HTTP/1.1 request/status line, header and body read/write helpers, replacing the wire-level
 * functionality previously provided by the Apache HttpCore {@code DefaultBHttpClientConnection} /
 * {@code DefaultBHttpServerConnection} classes used by {@link HttpSender} and {@link HttpReceiver}.
 * <p>
 * This is a deliberately narrow implementation, matching only what those two classes relied on: a request/status
 * line, a flat list of headers, and a body sized by the {@code Content-Length} header (always sent by
 * {@link HttpSender}, defaulting to {@code 0} otherwise). Chunked transfer encoding is not supported, as it was
 * never produced or consumed by this handler.
 */
final class Http11Wire {

    static final String CRLF = "\r\n";

    private Http11Wire() {
    }

    static void writeRequestLine(OutputStream out, String method, String path) throws IOException {
        writeLine(out, method + " " + path + " HTTP/1.1");
    }

    static void writeStatusLine(OutputStream out, int statusCode, String reasonPhrase) throws IOException {
        writeLine(out, "HTTP/1.1 " + statusCode + " " + reasonPhrase);
    }

    static void writeHeaders(OutputStream out, Map<String, String> headers) throws IOException {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            writeLine(out, header.getKey() + ": " + header.getValue());
        }
        writeLine(out, "");
    }

    static void writeBody(OutputStream out, byte[] body) throws IOException {
        if (body != null && body.length > 0) {
            out.write(body);
        }
    }

    private static void writeLine(OutputStream out, String line) throws IOException {
        out.write(line.getBytes(StandardCharsets.ISO_8859_1));
        out.write('\r');
        out.write('\n');
    }

    /**
     * Reads a single CRLF (or bare LF)-terminated line. Returns {@code null} if the stream is at EOF before any
     * bytes are read (i.e. the peer closed the connection without sending anything further).
     */
    static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int read;
        boolean any = false;
        while ((read = in.read()) != -1) {
            any = true;
            if (read == '\n') {
                break;
            }
            if (read != '\r') {
                buffer.write(read);
            }
        }
        if (!any) {
            return null;
        }
        return buffer.toString(StandardCharsets.ISO_8859_1);
    }

    static LinkedHashMap<String, String> readHeaders(InputStream in) throws IOException {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        String line;
        while ((line = readLine(in)) != null && !line.isEmpty()) {
            int separator = line.indexOf(':');
            if (separator > 0) {
                headers.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
            }
        }
        return headers;
    }

    static byte[] readBody(InputStream in, int contentLength) throws IOException {
        if (contentLength <= 0) {
            return new byte[0];
        }
        return in.readNBytes(contentLength);
    }

    /** Case-insensitive header lookup, matching the case-insensitive header semantics of HTTP. */
    static String getHeaderIgnoreCase(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    static String reasonPhrase(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 202 -> "Accepted";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "";
        };
    }
}
