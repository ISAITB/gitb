package com.gitb.messaging.server.tcp;

import com.gitb.messaging.SessionManager;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.messaging.server.AbstractMessagingServerWorker;
import com.gitb.messaging.server.NetworkingSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by serbay on 9/24/14.
 *
 * TCP messaging server worker used by TCP messaging server.
 *
 */
public class TCPMessagingServerWorker extends AbstractMessagingServerWorker {
	private static Logger logger = LoggerFactory.getLogger(TCPMessagingServerWorker.class);

	private TCPListenerThread listenerThread;
	private AtomicBoolean active;

    public TCPMessagingServerWorker(int port) {
	    super(port);
	    active = new AtomicBoolean(false);
    }

    public synchronized void start() throws IOException {
        if(listenerThread == null) {
            listenerThread = new TCPListenerThread(port);
            listenerThread.start();
        } else if(!isActive()) {
	        listenerThread.start();
        } else {
	        logger.debug("Already started to listen");
        }
    }

    public synchronized void stop() {
        if(listenerThread != null) {
            listenerThread.close();
            listenerThread = null;
        } else {
	        logger.debug("Already stopped listening");
        }
    }

	@Override
	public boolean isActive() {
		return active.get();
	}

    private void tryWaitingTransactions(Socket socket) {
        InetAddress address = socket.getInetAddress();
        NetworkingSessionManager.SessionInfo sessionInfo = networkingSessionManager.getSessionInfo(address);
        if(sessionInfo != null) {
            // The received communication is expected for a session.
            SessionContext sessionContext = SessionManager.getInstance().getSession(sessionInfo.getMessagingSessionId());
            TransactionContext transactionContext = sessionContext.getTransaction(address, socket.getLocalPort());
            if(transactionContext != null) {
                ITransactionReceiver receiver = transactionContext.getParameter(ITransactionReceiver.class);
                receiver.onReceive(socket);
            }
        } else {
            // The received communication is not expected.
            StringBuilder failedConnectionInfo = new StringBuilder();
            failedConnectionInfo.append("Received connection from [").append(address).append("] on port [").append(networkingSessionManager.getPort()).append("] but expected ");
            for (Map.Entry<InetAddress, NetworkingSessionManager.SessionInfo> entry: networkingSessionManager.getSessionMap().entrySet()) {
                failedConnectionInfo.append("[").append(entry.getKey()).append(" for ").append(entry.getValue().getTestSessionId()).append("] | ");
            }
            logger.warn(failedConnectionInfo.toString());
            try {
                logger.debug("Closing socket: " + socket);
                socket.close();
            } catch (IOException e) {
                // Ignore exception.
            }
        }
    }

	private class TCPReceiverThread extends Thread {
        private final Socket socket;

        private TCPReceiverThread(Socket socket) {
            super(TCPReceiverThread.class.getSimpleName() + ":" + socket.getInetAddress() + ":" + socket.getPort());
            this.socket = socket;
        }

        @Override
        public void run() {
            tryWaitingTransactions(socket);
        }
    }

    private class TCPListenerThread extends Thread {

        private int port;
        private ServerSocket serverSocket;

        private TCPListenerThread(int port) throws IOException {
            super(TCPListenerThread.class.getSimpleName() + ":" + port);
            this.port = port;
	        serverSocket = new ServerSocket(port);

	        logger.debug("New server created on: " + serverSocket);
        }

	    public void close() {
		    interrupt();
		    try {
			    serverSocket.close();
		    } catch (IOException e) {
			    logger.error("An error occurred while closing the socket", e);
		    }
	    }

        @Override
        public void run() {
            try {
	            active.set(true);

                while (!Thread.interrupted() && !serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();

                    logger.debug("New socket created: " + socket);

                    TCPReceiverThread receiverThread = new TCPReceiverThread(socket);
                    receiverThread.start();

                }
            } catch (Exception e) {
	            if(!serverSocket.isClosed()) {
		            logger.error("An error occurred", e);
	            }
            } finally {
	            logger.debug("Closing server created on: " + serverSocket);
	            if (!serverSocket.isClosed()) {
		            try {
			            serverSocket.close();
		            } catch (IOException e1) {
			            logger.error("An error occurred while closing the socket");
		            }
	            }
	            active.set(false);
            }
        }
    }
}
