package com.gitb.messaging.server.udp;

import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.SessionManager;
import com.gitb.messaging.model.udp.IDatagramReceiver;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.server.AbstractMessagingServerWorker;
import com.gitb.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by serbay on 9/24/14.
 * <p/>
 * UDP messaging server worker used by UDP messaging server.
 */
public class UDPMessagingServerWorker extends AbstractMessagingServerWorker {
    private static Logger logger = LoggerFactory.getLogger(UDPMessagingServerWorker.class);

    private UDPListenerThread listenerThread;
    private AtomicBoolean active;

    public UDPMessagingServerWorker(int port) {
        super(port);
        active = new AtomicBoolean(false);
    }

    public synchronized void start() {
        try {
            if (listenerThread == null) {
                listenerThread = new UDPListenerThread(port);
                listenerThread.listen();
                listenerThread.start();
            } else if (!isActive()) {
                listenerThread.listen();
                listenerThread.start();
            } else {
                logger.debug("Already started to listen");
            }
        } catch (SocketException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Could not open server at the port [" + port + "]"), e);
        }
    }

    public synchronized void stop() {
        if (listenerThread != null) {
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

    private class UDPListenerThread extends Thread {

        public static final int BUFFER_SIZE = 32 * 1024; // 32K

        private int port;
        private DatagramSocket datagramSocket;

        private UDPListenerThread(int port) {
            super(UDPListenerThread.class.getSimpleName() + ":" + port);
            this.port = port;
        }

        public void listen() throws SocketException {
            if (datagramSocket == null) {
                datagramSocket = new DatagramSocket(port);

                logger.debug("New server created on [" + datagramSocket.getLocalPort() + "]");
            }
        }

        public void close() {
            interrupt();
            datagramSocket.close();
        }

        @Override
        public void run() {
            try {
                active.set(true);

                while (!Thread.interrupted() && !datagramSocket.isClosed()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);

                    datagramSocket.receive(datagramPacket);

                    logger.debug("New message received from [" + datagramPacket.getAddress() + "]");

                    UDPReceiverThread receiverThread = new UDPReceiverThread(datagramSocket, datagramPacket);
                    receiverThread.start();
                }
            } catch (IOException e) {
                if (!datagramSocket.isClosed()) {
                    logger.error("Exception in ListenerThread", e);
                }
            } finally {
                logger.debug("Closing server created on: " + datagramSocket);
                if (datagramSocket != null && !datagramSocket.isClosed()) {
                    datagramSocket.close();
                }
                active.set(false);
            }
        }
    }

    private class UDPReceiverThread extends Thread {
        private final DatagramPacket datagramPacket;
        private final DatagramSocket socket;

        private UDPReceiverThread(DatagramSocket socket, DatagramPacket datagramPacket) {
            super(UDPReceiverThread.class.getSimpleName() + ":" + socket.getInetAddress() + ":" + socket.getPort());
            this.datagramPacket = datagramPacket;
            this.socket = socket;
        }

        @Override
        public void run() {
            InetAddress address = datagramPacket.getAddress();

            String sessionId = networkingSessionManager.getSessionId(address);

            if (sessionId != null) {
                SessionContext sessionContext = SessionManager.getInstance().getSession(sessionId);

                TransactionContext transactionContext = sessionContext.getTransaction(address, socket.getLocalPort());

                IDatagramReceiver receiver = transactionContext.getParameter(IDatagramReceiver.class);

                receiver.onReceive(socket, datagramPacket);
            }
        }
    }
}
