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

package com.gitb.engine.remote;

import com.gitb.engine.PropertyConstants;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;

import javax.xml.namespace.QName;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

import static com.gitb.engine.utils.TestCaseUtils.TEST_ENGINE_VERSION;

public class HttpHeaderHandler implements SOAPHandler<SOAPMessageContext> {

    private static final String WSS_URI_BASE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-";
    private static final String WSS_URI_CORE = WSS_URI_BASE + "wssecurity-secext-1.0.xsd";
    private static final String WSS_URI_UTIL = WSS_URI_BASE + "wssecurity-utility-1.0.xsd";
    private static final String WSS_URI_UTP = WSS_URI_BASE + "username-token-profile-1.0";
    private static final String WSS_URI_SOAP_MS = WSS_URI_BASE + "soap-message-security-1.0";
    private static final String GITB_BASE = "http://www.gitb.com";
    private static final String GITB_PREFIX = "gitb";

    private static final QName SECURITY_NAME =  new QName(WSS_URI_CORE, "Security", "wsse");
    private static final QName USERNAMETOKEN_NAME =  new QName(SECURITY_NAME.getNamespaceURI(), "UsernameToken", SECURITY_NAME.getPrefix());
    private static final QName USERNAME_NAME =  new QName(SECURITY_NAME.getNamespaceURI(), "Username", SECURITY_NAME.getPrefix());
    private static final QName PASSWORD_NAME =  new QName(SECURITY_NAME.getNamespaceURI(), "Password", SECURITY_NAME.getPrefix());
    private static final QName PASSWORDTYPE_NAME =  new QName("Type");
    private static final QName ENCODINGTYPE_NAME =  new QName("EncodingType");
    private static final QName UID_NAME =  new QName(WSS_URI_UTIL, "Id", "wsu");
    private static final QName CREATED_NAME =  new QName(UID_NAME.getNamespaceURI(), "Created", UID_NAME.getPrefix());
    private static final QName NONCE_NAME =  new QName(SECURITY_NAME.getNamespaceURI(), "Nonce", SECURITY_NAME.getPrefix());

    private static final QName TEST_STEP_ID_NAME =  new QName(GITB_BASE, "TestStepIdentifier", GITB_PREFIX);
    private static final QName TEST_SESSION_ID_NAME =  new QName(GITB_BASE, "TestSessionIdentifier", GITB_PREFIX);
    private static final QName TEST_CASE_ID_NAME =  new QName(GITB_BASE, "TestCaseIdentifier", GITB_PREFIX);
    private static final QName GITB_VERSION_NAME =  new QName(GITB_BASE, "TestEngineVersion", GITB_PREFIX);

    private static final String PASSWORDTYPE_TEXT_VALUE = WSS_URI_UTP+"#PasswordText";
    private static final String PASSWORDTYPE_DIGEST_VALUE = WSS_URI_UTP+"#PasswordDigest";
    private static final String ENCODINGTYPE_BASE64BINARY_VALUE = WSS_URI_SOAP_MS+"#Base64Binary";

    @Override
    public Set<QName> getHeaders() {
        return Collections.EMPTY_SET;
    }

    private Map<String, List<String>> getRequestHeaders(SOAPMessageContext context) {
        Map<String, List<String>> requestHeaders = (Map<String, List<String>>) context.get(MessageContext.HTTP_REQUEST_HEADERS);
        if (requestHeaders == null) {
            requestHeaders = new HashMap<>();
            context.put(MessageContext.HTTP_REQUEST_HEADERS, requestHeaders);
        }
        return requestHeaders;
    }

    private void addTestIdentifiers(SOAPMessageContext context, Properties callProperties) {
        String testSessionIdentifier = callProperties.getProperty(PropertyConstants.TEST_SESSION_ID);
        String testCaseIdentifier = callProperties.getProperty(PropertyConstants.TEST_CASE_ID);
        String testStepIdentifier = callProperties.getProperty(PropertyConstants.TEST_STEP_ID);
        boolean addTestSessionIdentifier = StringUtils.isNotBlank(testSessionIdentifier);
        boolean addTestCaseIdentifier = StringUtils.isNotBlank(testCaseIdentifier);
        boolean addTestStepIdentifier = StringUtils.isNotBlank(testStepIdentifier);
        try {
            SOAPEnvelope envelope = context.getMessage().getSOAPPart().getEnvelope();
            SOAPHeader header = envelope.getHeader();
            if (header == null) {
                header = envelope.addHeader();
            }
            if (addTestSessionIdentifier) {
                var element = header.addChildElement(TEST_SESSION_ID_NAME);
                element.addTextNode(testSessionIdentifier);
            }
            if (addTestCaseIdentifier) {
                var element = header.addChildElement(TEST_CASE_ID_NAME);
                element.addTextNode(testCaseIdentifier);
            }
            if (addTestStepIdentifier) {
                var element = header.addChildElement(TEST_STEP_ID_NAME);
                element.addTextNode(testStepIdentifier);
            }
            var versionElement = header.addChildElement(GITB_VERSION_NAME);
            versionElement.addTextNode(TEST_ENGINE_VERSION);
        } catch (SOAPException e) {
            throw new IllegalStateException("Error generating headers for test identifiers", e);
        }
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outboundProperty = (Boolean)context.get (MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (outboundProperty) {
            Properties callProperties = RemoteCallContext.getCallProperties();
            if (callProperties != null) {
                // Add test session and test case identifiers.
                addTestIdentifiers(context, callProperties);
                // Add basic HTTP authentication header.
                if (StringUtils.isNotBlank(callProperties.getProperty(PropertyConstants.AUTH_BASIC_USERNAME))) {
                    Map<String, List<String>> requestHeaders = getRequestHeaders(context);
                    String auth = callProperties.getProperty(PropertyConstants.AUTH_BASIC_USERNAME) + ":" + callProperties.getProperty(PropertyConstants.AUTH_BASIC_PASSWORD);
                    String authHeader = "Basic " + org.apache.commons.codec.binary.Base64.encodeBase64String(auth.getBytes());
                    requestHeaders.put(HttpHeaders.AUTHORIZATION, Collections.singletonList(authHeader));
                }
                // Add WS-Security UsernameToken.
                if (StringUtils.isNotBlank(callProperties.getProperty(PropertyConstants.AUTH_USERNAMETOKEN_USERNAME))) {
                    try {
                        SOAPEnvelope envelope = context.getMessage().getSOAPPart().getEnvelope();
                        SOAPHeader header = envelope.getHeader();
                        if (header == null) {
                            header = envelope.addHeader();
                        }
                        SOAPElement security;
                        Iterator it = header.getChildElements(SECURITY_NAME);
                        if (it.hasNext()) {
                            security = (SOAPElement)it.next();
                        } else {
                            security = header.addChildElement(SECURITY_NAME);
                        }
                        String createdValue = String.valueOf(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
                        SOAPElement usernameToken = security.addChildElement(USERNAMETOKEN_NAME);
                        usernameToken.addAttribute(UID_NAME,"UsernameToken");
                        // Add created timestamp
                        SOAPElement created = usernameToken.addChildElement(CREATED_NAME);
                        created.addTextNode(createdValue);
                        SOAPElement username = usernameToken.addChildElement(USERNAME_NAME);
                        username.addTextNode(callProperties.getProperty(PropertyConstants.AUTH_USERNAMETOKEN_USERNAME));
                        String passwordValue = callProperties.getProperty(PropertyConstants.AUTH_USERNAMETOKEN_PASSWORD);
                        String passwordType = callProperties.getProperty(PropertyConstants.AUTH_USERNAMETOKEN_PASSWORDTYPE);
                        if (StringUtils.isBlank(passwordType)) {
                            passwordType = PropertyConstants.AUTH_USERNAMETOKEN_PASSWORDTYPE_VALUE_DIGEST;
                        }
                        if (PropertyConstants.AUTH_USERNAMETOKEN_PASSWORDTYPE_VALUE_TEXT.equalsIgnoreCase(passwordType)) {
                            // TEXT
                            SOAPElement password = usernameToken.addChildElement(PASSWORD_NAME);
                            password.addAttribute(PASSWORDTYPE_NAME, PASSWORDTYPE_TEXT_VALUE);
                            password.addTextNode(passwordValue);
                        } else {
                            // DIGEST
                            String nonceValue = createNonce();
                            passwordValue = calculatePasswordDigest(nonceValue, createdValue, passwordValue);
                            // Add nonce
                            SOAPElement nonce = usernameToken.addChildElement(NONCE_NAME);
                            nonce.addAttribute(ENCODINGTYPE_NAME, ENCODINGTYPE_BASE64BINARY_VALUE);
                            nonce.addTextNode(nonceValue);
                            // Add password digest
                            SOAPElement password = usernameToken.addChildElement(PASSWORD_NAME);
                            password.addAttribute(PASSWORDTYPE_NAME, PASSWORDTYPE_DIGEST_VALUE);
                            password.addTextNode(passwordValue);
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Error generating UsernameToken header", e);
                    }
                }
            }
        }
        return true;
    }

    private String createNonce() {
        java.security.SecureRandom random;
        try {
            random = java.security.SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(System.currentTimeMillis());
            byte[] nonceBytes = new byte[16];
            random.nextBytes(nonceBytes);
            return new String(Base64.encodeBase64(nonceBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to make nonce", e);
        }
    }

    private String calculatePasswordDigest(String nonce, String created, String password) {
        try {
            byte[] passwordBase64Bytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] b1 = nonce != null ? Base64.decodeBase64(nonce) : new byte[0];
            byte[] b2 = created != null ? created.getBytes(StandardCharsets.UTF_8) : new byte[0];
            byte[] b4 = new byte[b1.length + b2.length + passwordBase64Bytes.length];
            int offset = 0;
            System.arraycopy(b1, 0, b4, offset, b1.length);
            offset += b1.length;
            System.arraycopy(b2, 0, b4, offset, b2.length);
            offset += b2.length;
            System.arraycopy(passwordBase64Bytes, 0, b4, offset, passwordBase64Bytes.length);
            byte[] digestBytes = MessageDigest.getInstance("SHA-1").digest(b4);
            return Base64.encodeBase64String(digestBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to make digest", e);
        }
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
        // Do nothing.
    }

}
