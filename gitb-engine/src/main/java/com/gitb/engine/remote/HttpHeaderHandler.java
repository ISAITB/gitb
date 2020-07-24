package com.gitb.engine.remote;

import com.gitb.engine.PropertyConstants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.HttpHeaders;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

public class HttpHeaderHandler implements SOAPHandler<SOAPMessageContext> {

    private static String WSS_URI_BASE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-";
    private static String WSS_URI_CORE = WSS_URI_BASE + "wssecurity-secext-1.0.xsd";
    private static String WSS_URI_UTIL = WSS_URI_BASE + "wssecurity-utility-1.0.xsd";
    private static String WSS_URI_UTP = WSS_URI_BASE + "username-token-profile-1.0";
    private static String WSS_URI_SOAP_MS = WSS_URI_BASE + "soap-message-security-1.0";

    private static QName SECURITY_NAME =  new QName(WSS_URI_CORE, "Security", "wsse");
    private static QName USERNAMETOKEN_NAME =  new QName(SECURITY_NAME.getNamespaceURI(), "UsernameToken", SECURITY_NAME.getPrefix());
    private static QName USERNAME_NAME =  new QName(SECURITY_NAME.getNamespaceURI(), "Username", SECURITY_NAME.getPrefix());
    private static QName PASSWORD_NAME =  new QName(SECURITY_NAME.getNamespaceURI(), "Password", SECURITY_NAME.getPrefix());
    private static QName PASSWORDTYPE_NAME =  new QName("Type");
    private static QName ENCODINGTYPE_NAME =  new QName("EncodingType");
    private static QName UID_NAME =  new QName(WSS_URI_UTIL, "Id", "wsu");
    private static QName CREATED_NAME =  new QName(UID_NAME.getNamespaceURI(), "Created", UID_NAME.getPrefix());
    private static QName NONCE_NAME =  new QName(SECURITY_NAME.getNamespaceURI(), "Nonce", SECURITY_NAME.getPrefix());

    private static String PASSWORDTYPE_TEXT_VALUE = WSS_URI_UTP+"#PasswordText";
    private static String PASSWORDTYPE_DIGEST_VALUE = WSS_URI_UTP+"#PasswordDigest";
    private static String ENCODINGTYPE_BASE64BINARY_VALUE = WSS_URI_SOAP_MS+"#Base64Binary";

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

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outboundProperty = (Boolean)context.get (MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (outboundProperty) {
            Properties callProperties = RemoteCallContext.getCallProperties();
            if (callProperties != null) {
                // Add basic HTTP authentication header.
                if (!StringUtils.isBlank(callProperties.getProperty(PropertyConstants.AUTH_BASIC_USERNAME))) {
                    Map<String, List<String>> requestHeaders = getRequestHeaders(context);
                    String auth = callProperties.getProperty(PropertyConstants.AUTH_BASIC_USERNAME) + ":" + callProperties.getProperty(PropertyConstants.AUTH_BASIC_PASSWORD);
                    String authHeader = "Basic " + org.apache.commons.codec.binary.Base64.encodeBase64String(auth.getBytes());
                    requestHeaders.put(HttpHeaders.AUTHORIZATION, Collections.singletonList(authHeader));
                }
                if (!StringUtils.isBlank(callProperties.getProperty(PropertyConstants.AUTH_USERNAMETOKEN_USERNAME))) {
                    // Add WS-Security UsernameToken.
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
                            passwordType = PropertyConstants.AUTH_USERNAMETOKEN_PASSWORDTYPE__VALUE_DIGEST;
                        }
                        if (PropertyConstants.AUTH_USERNAMETOKEN_PASSWORDTYPE__VALUE_TEXT.equalsIgnoreCase(passwordType)) {
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
            return new String(Base64.encodeBase64(nonceBytes), "UTF-8");
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
            String passwdDigest = Base64.encodeBase64String(digestBytes);
            return passwdDigest;
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
    }

}
