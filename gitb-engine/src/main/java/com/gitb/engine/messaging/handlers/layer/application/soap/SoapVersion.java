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

package com.gitb.engine.messaging.handlers.layer.application.soap;

import com.sun.xml.messaging.saaj.soap.ver1_1.SOAPMessageFactory1_1Impl;
import com.sun.xml.messaging.saaj.soap.ver1_2.SOAPMessageFactory1_2Impl;
import jakarta.xml.soap.MessageFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public enum SoapVersion {

    VERSION_1_1("1.1", "text/xml", Optional.of(Set.of("application/xml")), new SOAPMessageFactory1_1Impl()),
    VERSION_1_2("1.2", "application/soap+xml", Optional.empty(), new SOAPMessageFactory1_2Impl());

    private final String version;
    private final String contentType;
    private final Set<String> acceptedContentTypes;
    private final MessageFactory messageFactory;

    SoapVersion(String name, String contentType, Optional<Set<String>> acceptedContentTypes, MessageFactory messageFactory) {
        this.version = name;
        this.contentType = contentType;
        this.messageFactory = messageFactory;
        this.acceptedContentTypes = acceptedContentTypes
                .map(types -> {
                    Set<String> newTypes = new HashSet<>(types);
                    newTypes.add(contentType);
                    return newTypes;
                }).orElse(Set.of(contentType));
    }

    public static SoapVersion forInput(String inputValue) {
        if (VERSION_1_1.getVersion().equals(inputValue)) {
            return VERSION_1_1;
        } else if (VERSION_1_2.getVersion().equals(inputValue)) {
            return VERSION_1_2;
        } else {
            throw new IllegalArgumentException("If set, the SOAP version must be set either as [%s] or [%s].".formatted(VERSION_1_1.version, VERSION_1_2.version));
        }
    }

    public static SoapVersion forContentTypeHeaders(Iterable<String> headers) {
        return forContentTypeHeader(String.join(", ", headers));
    }

    public static SoapVersion forContentTypeHeader(String header) {
        return VERSION_1_2.acceptedContentTypes.stream()
                .filter(header::contains)
                .findAny()
                .map(v -> SoapVersion.VERSION_1_2)
                .orElse(SoapVersion.VERSION_1_1);
    }

    public String getVersion() {
        return version;
    }

    public String getContentType() {
        return contentType;
    }

    public MessageFactory buildMessageFactory() {
        return messageFactory;
    }
}
