package com.gitb.engine.messaging.handlers;

import com.gitb.engine.messaging.handlers.server.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static com.gitb.engine.TestEngineConfiguration.DEFAULT_MESSAGING_CONFIGURATION;

/**
 * Created by senan on 11.11.2014.
 */
public class KeyStoreFactory {

    private static final String KEYSTORE_TYPE = "JKS";
    private static final String CERTIFICATE_TYPE = "X.509";

    private static KeyStoreFactory instance;

    private KeyStore keyStore;

    private String defaultAlias;
    private String keyStoreLocation;
    private String keyStorePassword;

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public X509Certificate getCertificate() {
        try {
            return (X509Certificate) keyStore.getCertificate(defaultAlias);
        } catch (KeyStoreException e) {
            throw new IllegalStateException("Error while returning certificate", e);
        }
    }

    public X509Certificate generateCertificate(byte[] bytes) {
        InputStream stream = new ByteArrayInputStream(bytes);
        return generateCertificate(stream);
    }

    public X509Certificate generateCertificate(InputStream stream) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE);
            return (X509Certificate)certFactory.generateCertificate(stream);
        } catch (CertificateException e) {
            throw new IllegalStateException("Error while generating certificate", e);
        }
    }

    public PrivateKey getPrivateKey() {
        try {
            return (PrivateKey) keyStore.getKey(defaultAlias, keyStorePassword.toCharArray());
        } catch (Exception e) {
            throw new IllegalStateException("Error while returning private key", e);
        }
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getDefaultAlias() {
        return defaultAlias;
    }

    private KeyStoreFactory() {
        Configuration defaultConfiguration = DEFAULT_MESSAGING_CONFIGURATION;

        try {
            defaultAlias     = defaultConfiguration.getDefaultAlias();
            keyStorePassword = defaultConfiguration.getKeystorePassword();
            keyStoreLocation = defaultConfiguration.getKeystoreLocation();

            keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(getClass().getResourceAsStream("/" + keyStoreLocation),
                          keyStorePassword.toCharArray());
        } catch (Exception e) {
            throw new IllegalStateException("Error while initializing KeyStore", e);
        }
    }

    public static KeyStoreFactory getInstance() {
        if(instance == null) {
            instance = new KeyStoreFactory();
        }

        return instance;
    }
}
