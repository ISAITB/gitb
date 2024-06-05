package com.gitb.engine.messaging.handlers.layer.application.soap;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public enum SoapVersion {

    VERSION_1_1("1.1", "text/xml", Optional.of(Set.of("application/xml")), SOAPConstants.SOAP_1_1_PROTOCOL),
    VERSION_1_2("1.2", "application/soap+xml", Optional.empty(), SOAPConstants.SOAP_1_2_PROTOCOL);

    private final String version;
    private final String contentType;
    private final Set<String> acceptedContentTypes;
    private final String soapProtocol;

    SoapVersion(String name, String contentType, Optional<Set<String>> acceptedContentTypes, String soapProtocol) {
        this.version = name;
        this.contentType = contentType;
        this.soapProtocol = soapProtocol;
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
        try {
            return  MessageFactory.newInstance(soapProtocol);
        } catch (SOAPException e) {
            throw new IllegalStateException("Unable to create SOAP message factory for version [%s]".formatted(version), e);
        }
    }
}
