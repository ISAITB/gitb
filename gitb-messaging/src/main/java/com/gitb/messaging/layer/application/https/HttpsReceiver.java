package com.gitb.messaging.layer.application.https;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.SecurityUtils;
import com.gitb.messaging.layer.application.http.HttpReceiver;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import java.util.List;

/**
 * Created by senan on 07.11.2014.
 */
public class HttpsReceiver extends HttpReceiver {
    private Logger logger = LoggerFactory.getLogger(HttpsReceiver.class);

    public HttpsReceiver(SessionContext session, TransactionContext transaction) {
        super(session, transaction);
    }

    @Override
    public Message receive(List<Configuration> configurations, Message inputs) throws Exception {
        //use the socket retrieved from the transaction
        socket = getSocket();

        //if the socket is null, that means transaction has just begun, so create new
        //below code blocks until a socket is created
        if(socket == null) {
            waitUntilMessageReceived();
        }

        //secure this socket if it is not SSL secured
        if(!(socket instanceof SSLSocket)) {//no need to create if we already have one
            socket = SecurityUtils.secureSocket(transaction, socket);
            ((SSLSocket) socket).setUseClientMode(false); //do not use client mode for handshaking since it is a server socket
        }

        return super.receive(configurations, inputs);
    }
}
