package com.gitb.messaging.layer;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.ServerUtils;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.model.udp.IDatagramSender;
import com.gitb.utils.ConfigurationUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

/**
 * Created by serbay.
 */
public abstract class AbstractDatagramSender implements IDatagramSender {

	protected final SessionContext session;
	protected final TransactionContext transaction;

	protected AbstractDatagramSender(SessionContext session, TransactionContext transaction) {
		this.session = session;
		this.transaction = transaction;
	}

	@Override
	public Message send(List<Configuration> configurations, Message message) throws Exception {
		DatagramSocket socket = new DatagramSocket();
		DatagramPacket packet = new DatagramPacket(new byte[]{}, 0);

		transaction.setParameter(DatagramSocket.class, socket);
		transaction.setParameter(DatagramPacket.class, packet);

        return null;
	}

	@Override
	public void onEnd() throws Exception {
		/*DatagramSocket socket = transaction.getParameter(DatagramSocket.class);

		socket.close();*/
	}
}
