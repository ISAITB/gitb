package com.gitb.messaging.layer;

import com.gitb.messaging.Message;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.model.udp.IDatagramReceiver;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by serbay.
 */
public abstract class AbstractDatagramReceiver implements IDatagramReceiver {

	protected final SessionContext session;
	protected final TransactionContext transaction;

	private final Lock lock;
	private final Condition messageReceived;
	private Exception error;

	protected AbstractDatagramReceiver(SessionContext session, TransactionContext transaction) {
		this.session = session;
		this.transaction = transaction;

		this.lock = new ReentrantLock();
		this.messageReceived = lock.newCondition();
		this.error = null;
	}

	protected Marker addMarker() {
		return MarkerFactory.getDetachedMarker(session.getTestSessionId());
	}

	protected void waitUntilMessageReceived() throws Exception {
		lock.lock();
		try {
			messageReceived.await();

			if(error != null) {
				throw error;
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void onReceive(DatagramSocket socket, DatagramPacket datagramPacket) {

		lock.lock();
		try {
			transaction.setParameter(DatagramSocket.class, socket);
			transaction.setParameter(DatagramPacket.class, datagramPacket);
			messageReceived.signal();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void onEnd() throws Exception {

	}

	@Override
	public void onError(Exception cause) {
		lock.lock();
		this.error = cause;
		try {
			messageReceived.signal();
		} finally {
			lock.unlock();
		}
	}
}
