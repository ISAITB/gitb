package com.gitb.messaging.layer;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.ServerUtils;
import com.gitb.messaging.model.tcp.ITransactionSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

/**
 * Created by serbay on 9/25/14.
 */
public abstract class AbstractTransactionSender implements ITransactionSender {
	private static final Logger logger = LoggerFactory.getLogger(AbstractTransactionSender.class);

	protected final SessionContext session;
	protected final TransactionContext transaction;

	public AbstractTransactionSender(SessionContext session, TransactionContext transaction) {
		this.session = session;
		this.transaction = transaction;
	}

	@Override
	public Message send(List<Configuration> configurations, Message message) throws Exception {
		Socket socket = getSocket();
		if(socket == null) {
			// create a client socket
			ActorConfiguration actorConfiguration = transaction.getWith();

			Configuration ipAddressConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.IP_ADDRESS_CONFIG_NAME);
			Configuration portConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.PORT_CONFIG_NAME);

			socket = new Socket(InetAddress.getByName(ipAddressConfig.getValue()), Integer.parseInt(portConfig.getValue()));

            transaction.setParameter(Socket.class, socket);
		}

        return null;
	}

	@Override
	public void onEnd() throws Exception {
		Socket socket = getSocket();
		if(socket != null && !socket.isClosed()) {
			logger.debug("Closing socket: " + socket);
			socket.close();
		}
	}

	protected Socket getSocket() {
		return transaction.getParameter(Socket.class);
	}
}
