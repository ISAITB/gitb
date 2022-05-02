package com.gitb.engine.messaging.handlers.layer;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.engine.messaging.handlers.model.udp.IDatagramSender;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
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

	protected Marker addMarker() {
		return MarkerFactory.getDetachedMarker(session.getTestSessionId());
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
