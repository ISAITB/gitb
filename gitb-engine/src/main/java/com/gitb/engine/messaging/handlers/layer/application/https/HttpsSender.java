package com.gitb.engine.messaging.handlers.layer.application.https;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.ServerUtils;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpSender;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

/**
 * Created by senan on 07.11.2014.
 */
public class HttpsSender extends HttpSender {
    private Logger logger = LoggerFactory.getLogger(HttpsSender.class);

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
            socket = sf.createSocket(InetAddress.getByName(ipAddressConfig.getValue()),
                    Integer.parseInt(portConfig.getValue()));

            transaction.setParameter(Socket.class, socket);
        }

        super.send(configurations, message);
        return message;
    }
}
