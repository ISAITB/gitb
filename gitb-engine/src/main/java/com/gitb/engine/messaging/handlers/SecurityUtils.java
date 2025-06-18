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

package com.gitb.engine.messaging.handlers;


import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.helger.commons.ws.TrustManagerTrustAll;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;

public class SecurityUtils {

    private static final String PKI_ALGORITHM     = "SunX509";
    private static final String SECURE_ALGORITHM  = "TLSv1.2";

    public static SSLContext createSSLContext() {
        SSLContext sslContext = null;

        try {
            KeyStore keyStore = KeyStoreFactory.getInstance().getKeyStore();

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(PKI_ALGORITHM);
            kmf.init(keyStore, KeyStoreFactory.getInstance().getKeyStorePassword().toCharArray());

            TrustManager[] trustManagers = new TrustManager [] { new TrustManagerTrustAll() };

            sslContext = SSLContext.getInstance(SECURE_ALGORITHM);
            sslContext.init(kmf.getKeyManagers(), trustManagers, null);
        } catch (Exception e) {
            throw new IllegalStateException("Exception while creating SSLContext in HttpsMessagingHandler", e);
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
