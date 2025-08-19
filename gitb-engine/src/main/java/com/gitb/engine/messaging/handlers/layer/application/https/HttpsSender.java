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

package com.gitb.engine.messaging.handlers.layer.application.https;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.engine.messaging.handlers.ServerUtils;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpSender;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.messaging.Message;
import com.gitb.utils.ConfigurationUtils;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Objects;

/**
 * Created by senan on 07.11.2014.
 */
public class HttpsSender extends HttpSender {

    public HttpsSender(SessionContext session, TransactionContext transaction) {
        super(session, transaction);
    }

    @Override
    public Message send(List<Configuration> configurations, Message message) throws Exception {

        //use the socket retrieved from the transaction
	    Socket socket = getSocket();

        //secure this socket if it is not SSL secured
	    if(!(socket instanceof SSLSocket)) { //no need to create if we already have one
            SSLContext sslContext = transaction.getParameter(SSLContext.class);

            ActorConfiguration actorConfiguration = transaction.getWith();
            Configuration ipAddressConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.IP_ADDRESS_CONFIG_NAME);
            Configuration portConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.PORT_CONFIG_NAME);

            SocketFactory sf = sslContext.getSocketFactory();
            socket = sf.createSocket(InetAddress.getByName(Objects.requireNonNull(ipAddressConfig).getValue()),
                    Integer.parseInt(Objects.requireNonNull(portConfig).getValue()));

            transaction.setParameter(Socket.class, socket);
        }

        super.send(configurations, message);
        return message;
    }
}
