/*
 * Copyright (C) 2025 European Union
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class MultipartFormDataBodyPublisher implements BodyPublisher {

    private static String nextBoundary() {
        var random = new BigInteger(128, ThreadLocalRandom.current());
        try (var formatter = new Formatter()) {
            return formatter.format("-----------------------------%039d", random).toString();
        }
    }

    private final String boundary = nextBoundary();
    private final List<Part> parts = new ArrayList<>();
    private final Charset charset;
    private final BodyPublisher delegate;

    /**
     * Construct {@link MultipartFormDataBodyPublisher}
     */
    public MultipartFormDataBodyPublisher() {
        this(StandardCharsets.UTF_8);
    }

    /**
     * Construct {@link MultipartFormDataBodyPublisher}
     *
     * @param charset
     *            character encoding
     */
    public MultipartFormDataBodyPublisher(Charset charset) {
        this.charset = charset;
        this.delegate = BodyPublishers.ofInputStream(() -> Channels.newInputStream(new MultipartFormDataChannel(this.boundary, this.parts, this.charset)));
    }

    private MultipartFormDataBodyPublisher add(Part part) {
        this.parts.add(part);
        return this;
    }

    /**
     * Add part.
     *
     * @param name
     *            field name
     * @param value
     *            field value
     * @return this
     */
    public MultipartFormDataBodyPublisher add(String name, String value) {
        return this.add(new StringPart(name, value, this.charset));
    }

    /**
     * Add part. Content using specified path.
     *
     * @param name
     *            field name
     * @param path
     *            field value
     * @return this
     */
    public MultipartFormDataBodyPublisher addFile(String name, Path path) {
        return this.add(new FilePart(name, path));
    }

    /**
     * Add part. Content using specified path.
     *
     * @param name
     *            field name
     * @param path
     *            field value
     * @param contentType
     *            Content-Type
     * @return this
     */
    public MultipartFormDataBodyPublisher addFile(String name, Path path, String contentType) {
        return this.add(new FilePart(name, path, contentType));
    }

    /**
     * Add part with {@link InputStream}
     *
     * @param name
     *            field name
     * @param filename
     *            file name
     * @param supplier
     *            field value
     * @return this
     */
    public MultipartFormDataBodyPublisher addStream(String name, String filename, Supplier<InputStream> supplier) {
        return this.add(new StreamPart(name, filename, () -> Channels.newChannel(supplier.get())));
    }

    /**
     * Add part with {@link InputStream}
     *
     * @param name
     *            field name
     * @param filename
     *            file name
     * @param supplier
     *            field value
     * @param contentType
     *            Content-Type
     * @return this
     */
    public MultipartFormDataBodyPublisher addStream(String name, String filename, Supplier<InputStream> supplier,
                                                    String contentType) {
        return this.add(new StreamPart(name, filename, () -> Channels.newChannel(supplier.get()), contentType));
    }

    /**
     * Add part with {@link ReadableByteChannel}
     *
     * @param name
     *            field name
     * @param filename
     *            file name
     * @param supplier
     *            field value
     * @return this
     */
    public MultipartFormDataBodyPublisher addChannel(String name, String filename,
                                                     Supplier<ReadableByteChannel> supplier) {
        return this.add(new StreamPart(name, filename, supplier));
    }

    /**
     * Add part with {@link ReadableByteChannel}
     *
     * @param name
     *            field name
     * @param filename
     *            file name
     * @param supplier
     *            field value
     * @param contentType
     *            Content-Type
     * @return this
     */
    public MultipartFormDataBodyPublisher addChannel(String name, String filename,
                                                     Supplier<ReadableByteChannel> supplier, String contentType) {
        return this.add(new StreamPart(name, filename, supplier, contentType));
    }

    /**
     * Get Content-Type
     *
     * @return Content-Type
     */
    public String contentType() {
        try (var formatter = new Formatter()) {
            return formatter.format("multipart/form-data; boundary=%s", this.boundary).toString();
        }
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        delegate.subscribe(s);
    }

    @Override
    public long contentLength() {
        return delegate.contentLength();
    }

}

interface Part {
    String name();

    default Optional<String> filename() {
        return Optional.empty();
    }

    default Optional<String> contentType() {
        return Optional.empty();
    }

    ReadableByteChannel open() throws IOException;
}

class StringPart implements Part {
    private final String name;
    private final String value;
    private final Charset charset;

    StringPart(String name, String value, Charset charset) {
        this.name = name;
        this.value = value;
        this.charset = charset;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public ReadableByteChannel open() {
        var input = new ByteArrayInputStream(this.value.getBytes(this.charset));
        return Channels.newChannel(input);
    }
}

class StreamPart implements Part {
    private final String name;
    private final String filename;
    private final Supplier<ReadableByteChannel> supplier;
    private final String contentType;

    StreamPart(String name, String filename, Supplier<ReadableByteChannel> supplier, String contentType) {
        this.name = name;
        this.filename = filename;
        this.supplier = supplier;
        this.contentType = contentType;
    }

    StreamPart(String name, String filename, Supplier<ReadableByteChannel> supplier) {
        this(name, filename, supplier, "application/octet-stream");
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Optional<String> filename() {
        return Optional.of(filename);
    }

    @Override
    public Optional<String> contentType() {
        return Optional.of(this.contentType);
    }

    @Override
    public ReadableByteChannel open() {
        return this.supplier.get();
    }
}

class FilePart implements Part {
    private final String name;
    private final Path path;
    private final String contentType;

    FilePart(String name, Path path, String contentType) {
        this.name = name;
        this.path = path;
        this.contentType = contentType;
    }

    FilePart(String name, Path path) {
        this(name, path, "application/octet-stream");
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Optional<String> filename() {
        return Optional.of(this.path.getFileName().toString());
    }

    @Override
    public Optional<String> contentType() {
        return Optional.of(this.contentType);
    }

    @Override
    public ReadableByteChannel open() throws IOException {
        return Files.newByteChannel(this.path);
    }
}

enum State {
    BOUNDARY, HEADERS, BODY, DONE,
}

class MultipartFormDataChannel implements ReadableByteChannel {
    private boolean closed = false;
    private State state = State.BOUNDARY;
    private final String boundary;
    private final Iterator<Part> parts;
    private ByteBuffer buf = ByteBuffer.allocate(0);
    private Part current = null;
    private ReadableByteChannel channel = null;
    private final Charset charset;

    MultipartFormDataChannel(String boundary, Iterable<Part> parts, Charset charset) {
        this.boundary = boundary;
        this.parts = parts.iterator();
        this.charset = charset;
    }

    @Override
    public void close() throws IOException {
        if (this.channel != null) {
            this.channel.close();
            this.channel = null;
        }
        this.closed = true;
    }

    @Override
    public boolean isOpen() {
        return !this.closed;
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        while (true) {
            if (this.buf.hasRemaining()) {
                var n = Math.min(this.buf.remaining(), buf.remaining());
                var slice = this.buf.slice();
                slice.limit(n);
                buf.put(slice);
                this.buf.position(this.buf.position() + n);
                return n;
            }

            switch (this.state) {
                case BOUNDARY:
                    if (this.parts.hasNext()) {
                        this.current = this.parts.next();
                        this.buf = ByteBuffer.wrap(("--" + this.boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                        this.state = State.HEADERS;
                    } else {
                        this.buf = ByteBuffer.wrap(("--" + this.boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                        this.state = State.DONE;
                    }
                    break;

                case HEADERS:
                    this.buf = ByteBuffer.wrap(this.currentHeaders().getBytes(this.charset));
                    this.state = State.BODY;
                    break;

                case BODY:
                    if (this.channel == null) {
                        this.channel = this.current.open();
                    }

                    var n = this.channel.read(buf);
                    if (n == -1) {
                        this.channel.close();
                        this.channel = null;
                        this.buf = ByteBuffer.wrap("\r\n".getBytes(StandardCharsets.UTF_8));
                        this.state = State.BOUNDARY;
                    } else {
                        return n;
                    }
                    break;

                case DONE:
                    return -1;
            }
        }
    }

    static String escape(String s) {
        return s.replace("\"", "\\\"");
    }

    String currentHeaders() {
        var current = this.current;

        if (current == null) {
            throw new IllegalStateException();
        }

        var contentType = current.contentType();
        var filename = current.filename();
        if (contentType.isPresent() && filename.isPresent()) {
            var format = new StringJoiner("\r\n", "", "\r\n")
                    .add("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"").add("Content-Type: %s")
                    .toString();
            try (var formatter = new Formatter()) {
                return formatter
                        .format(format, escape(current.name()), escape(filename.get()), escape(contentType.get())) + "\r\n";
            }

        } else if (contentType.isPresent()) {
            var format = new StringJoiner("\r\n", "", "\r\n").add("Content-Disposition: form-data; name=\"%s\"")
                    .add("Content-Type: %s").toString();
            try (var formatter = new Formatter()) {
                return formatter.format(format, escape(current.name()), escape(contentType.get())) + "\r\n";
                // escape
            }

        } else if (filename.isPresent()) {
            var format = new StringJoiner("\r\n", "", "\r\n")
                    .add("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"").toString();
            try (var formatter = new Formatter()) {
                return formatter.format(format, escape(current.name()), escape(filename.get())) + "\r\n";
                // escape
            }

        } else {
            var format = new StringJoiner("\r\n", "", "\r\n").add("Content-Disposition: form-data; name=\"%s\"")
                    .toString();
            try (var formatter = new Formatter()) {
                return formatter.format(format, escape(current.name())) + "\r\n";
            }
        }
    }
}
