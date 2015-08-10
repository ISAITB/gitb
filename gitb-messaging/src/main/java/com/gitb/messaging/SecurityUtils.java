package com.gitb.messaging;


import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.model.TransactionContext;
import com.helger.as2lib.util.http.DoNothingTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;

public class SecurityUtils {

    private static Logger logger = LoggerFactory.getLogger(SecurityUtils.class);

    private static final String PKI_ALGORITHM     = "SunX509";
    private static final String SECURE_ALGORITHM  = "TLSv1.2";

    public static SSLContext createSSLContext() {
        SSLContext sslContext = null;

        try {
            KeyStore keyStore = KeyStoreFactory.getInstance().getKeyStore();

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(PKI_ALGORITHM);
            kmf.init(keyStore, KeyStoreFactory.getInstance().getKeyStorePassword().toCharArray());

            TrustManager[] trustManagers = new TrustManager [] { new DoNothingTrustManager() };

            sslContext = SSLContext.getInstance(SECURE_ALGORITHM);
            sslContext.init(kmf.getKeyManagers(), trustManagers, null);
        } catch (Exception e) {
            logger.error("Exception while creating SSLContext in HttpsMessagingHandler", e);
        }

        return sslContext;
    }

    public static SSLSocket secureSocket(SSLContext sslContext, Socket socket) throws IOException {
        SSLSocketFactory sf = sslContext.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) sf.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), false);
        return sslSocket;
    }

    public static SSLSocket secureSocket(TransactionContext transactionContext, Socket socket) throws IOException {
        SSLContext sslContext = transactionContext.getParameter(SSLContext.class);
        if(sslContext == null) {
            throw new GITBEngineInternalError("No SSLContext has been saved into this transaction");
        }

        //secure the socket
        SSLSocket sslSocket = secureSocket(sslContext, socket);

        //save new secured socket into this transaction
        transactionContext.setParameter(Socket.class, sslSocket);
        return sslSocket;
    }
}
