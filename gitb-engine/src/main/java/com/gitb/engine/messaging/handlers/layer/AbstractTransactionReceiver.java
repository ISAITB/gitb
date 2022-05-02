package com.gitb.engine.messaging.handlers.layer;

import com.gitb.engine.messaging.handlers.model.tcp.ITransactionReceiver;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by serbay on 9/25/14.
 */
public abstract class AbstractTransactionReceiver implements ITransactionReceiver {
	private static Logger logger = LoggerFactory.getLogger(AbstractTransactionReceiver.class);

	protected Socket socket;

	protected final SessionContext session;
	protected final TransactionContext transaction;

	private final Lock lock;
	private final Condition messageReceived;
	private Exception error;

	public AbstractTransactionReceiver(SessionContext session, TransactionContext transaction) {
		this.session = session;
		this.transaction = transaction;
		this.lock = new ReentrantLock();
		this.messageReceived = lock.newCondition();
		this.error = null;

		Socket socket = getSocket();
		if(socket != null) {
			this.socket = socket;
		}
	}

	public Marker addMarker() {
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
	public void onReceive(Socket socket) {
		lock.lock();
		try {
			if(this.socket == null) {
				this.socket = socket;
                this.transaction.setParameter(Socket.class, socket);
			}
			messageReceived.signal();
		} finally {
			lock.unlock();
		}
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

	@Override
	public void onEnd() throws Exception {
		if(socket != null && !socket.isClosed()) {
			logger.debug(addMarker(), "Closing socket: " + socket);
			socket.close();
		}
	}

    protected Socket getSocket(){
        return transaction.getParameter(Socket.class);
    }
}
