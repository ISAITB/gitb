package com.gitb.messaging.layer.application.oftp2;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.SecurityUtils;
import com.gitb.messaging.layer.AbstractTransactionSender;
import com.gitb.messaging.layer.transport.tcp.TCPMessagingHandler;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class OFTP2Sender extends AbstractTransactionSender {

    private static final Logger logger = LoggerFactory.getLogger(OFTP2Sender.class);

    public static boolean SECURE_CONNECTION = true;
    public static int BUFFER_SIZE = 4096;
    public static int WINDOW_SIZE = 64;
    public static boolean COMPRESSION_SUPPORT = false;
    public static boolean RESTART_SUPPORT = false;
    public static boolean SPECIAL_LOGIC = false;

    public OFTP2Sender(SessionContext session, TransactionContext transaction) {
        super(session, transaction);
    }

    @Override
    public Message send(List<Configuration> configurations, Message message) throws Exception {
        //this ensures that a socket is created and saved into the transaction
        super.send(configurations, message);

        parseConfigurations(configurations);

        Socket socket = getSocket();

        if(SECURE_CONNECTION) {
            socket = SecurityUtils.secureSocket(transaction, socket);
            ((SSLSocket) socket).setUseClientMode(true);
        }

        //get inputs
        byte[] fileContent = (byte[]) message.getFragments().get(OFTP2MessagingHandler.FILE_CONTENT_FIELD_NAME).getValue();
        String fileName    = (String) message.getFragments().get(OFTP2MessagingHandler.FILE_NAME_FIELD_NAME).getValue();

        InputStream inputStream   = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        //receive and validate SSRM (Start Session Ready Message)
        byte[] ssrm = OFTP2MessagingHandler.readOFTPMessage(inputStream);
        OFTP2MessagingHandler.validateCommandIdentifier(ssrm, OFTP2Constants.SSRM);
        logger.debug("SSRM (Start Session Ready Message) received...");

        //send SSID (Start Session)
        String usercode = ConfigurationUtils.getConfiguration(transaction.getWith().getConfig(), OFTP2MessagingHandler.USERCODE_CONFIG_NAME).getValue();
        String password = ConfigurationUtils.getConfiguration(transaction.getWith().getConfig(), OFTP2MessagingHandler.PASSWORD_CONFIG_NAME).getValue();
        byte[] ssid = OFTP2MessagingHandler.createSSIDMessage(usercode, password, BUFFER_SIZE,
                OFTP2Constants.SENDER_ONLY_TRANSFER_MODE, COMPRESSION_SUPPORT, RESTART_SUPPORT,
                SPECIAL_LOGIC, WINDOW_SIZE, false, null, null);
        TCPMessagingHandler.sendBytes(outputStream, OFTP2MessagingHandler.encodeMessage(ssid));

        //receive and validate SSID (Start Session)
        ssid = OFTP2MessagingHandler.readOFTPMessage(inputStream);
        OFTP2MessagingHandler.validateSSID(ssid);
        logger.debug("SSID (Start Session) received...");

        //send SFID (Start File)
        long fileSize = OFTP2MessagingHandler.computeVirtualFileSize(fileContent.length);
        byte[] sfid = OFTP2MessagingHandler.createSFIDMessage(fileName, new Date(), usercode, usercode, fileSize);
        TCPMessagingHandler.sendBytes(outputStream, OFTP2MessagingHandler.encodeMessage(sfid));
        logger.debug("SFID (Start File) sent...");

        //receive and validate SFPA (Start File Positive Answer)
        byte[] sfpa = OFTP2MessagingHandler.readOFTPMessage(inputStream);
        OFTP2MessagingHandler.validateCommandIdentifier(sfpa, OFTP2Constants.SFPA);
        logger.debug("SFPA (Start File Positive Answer) received...");

        //send DATA
        sendData(outputStream, fileContent);
        logger.debug("DATA sending finished...");

        //send EFID (End File)
        byte[] efid = OFTP2MessagingHandler.createEFIDMessage(0, fileContent.length);
        TCPMessagingHandler.sendBytes(outputStream, OFTP2MessagingHandler.encodeMessage(efid));
        logger.debug("EFID (End File) sent...");

        //receive EFPA (End File Postive Answer)
        byte[] efpa = OFTP2MessagingHandler.readOFTPMessage(inputStream);
        OFTP2MessagingHandler.validateCommandIdentifier(efpa, OFTP2Constants.EFPA);
        logger.debug("EFPA (End File Positive Answer) received...");

        //send  CD
        byte[] cd = OFTP2MessagingHandler.createCDMessage();
        TCPMessagingHandler.sendBytes(outputStream, OFTP2MessagingHandler.encodeMessage(cd));
        logger.debug("CD (Change Direction) sent...");

        //receive ESID
        byte[] esid = OFTP2MessagingHandler.readOFTPMessage(inputStream);
        OFTP2MessagingHandler.validateCommandIdentifier(esid, OFTP2Constants.ESID);
        logger.debug("ESID (End Session) received...");

        return message;
    }

    private void sendData(OutputStream outputStream, byte[] data) throws IOException {
        int dataIndex = 0;

        while(dataIndex < data.length) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            buffer.write(OFTP2Constants.DATA);

            int freeSpace;

            while ((freeSpace = BUFFER_SIZE - buffer.size()) > 0) {
                boolean endOfRecord = false;

                //get subrecord
                int subrecordSize = Math.min(OFTP2Constants.MAX_SUBRECORD_HEADER_SIZE, freeSpace-1);

                if(data.length - dataIndex < subrecordSize) {
                    endOfRecord = true;
                    subrecordSize = data.length - dataIndex;
                }

                byte[] subRecord = Arrays.copyOfRange(data, dataIndex, dataIndex+subrecordSize);

                //create subrecord header
                boolean compression = false;

                short subrecordHeader = 0;
                subrecordHeader += (endOfRecord ? 0x80 : 0x00);
                subrecordHeader += (compression ? 0x40 : 0x00);
                subrecordHeader += ((subrecordSize & 0xff ) & 0x3f);

                //write subrecord to buffer
                buffer.write(subrecordHeader);
                buffer.write(subRecord);

                //iterate
                dataIndex += subrecordSize;

                if(endOfRecord)
                    break;
            }

            //send data buffer
            TCPMessagingHandler.sendBytes(outputStream, OFTP2MessagingHandler.encodeMessage(buffer.toByteArray()));
            logger.debug("DATA sent...");
        }
    }

    private void parseConfigurations(List<Configuration> configurations) {
        Configuration secureConnection = ConfigurationUtils.getConfiguration(configurations, OFTP2MessagingHandler.SECURE_CONNECTION_CONFIG_NAME);
        if(secureConnection != null) {
            SECURE_CONNECTION = Boolean.parseBoolean(secureConnection.getValue());
        }

        Configuration bufferSize = ConfigurationUtils.getConfiguration(configurations, OFTP2MessagingHandler.BUFFER_SIZE_CONFIG_NAME);
        if(bufferSize != null) {
            BUFFER_SIZE = Integer.parseInt(bufferSize.getValue());
        }

        Configuration windowSize = ConfigurationUtils.getConfiguration(configurations, OFTP2MessagingHandler.WINDOW_SIZE_CONFIG_NAME);
        if(windowSize != null) {
            WINDOW_SIZE = Integer.parseInt(windowSize.getValue());
        }

        Configuration compressionSupport = ConfigurationUtils.getConfiguration(configurations, OFTP2MessagingHandler.COMPRESSION_SUPPORT_CONFIG_NAME);
        if(compressionSupport != null) {
            COMPRESSION_SUPPORT = Boolean.parseBoolean(compressionSupport.getValue());
        }

        Configuration restartSupport = ConfigurationUtils.getConfiguration(configurations, OFTP2MessagingHandler.RESTART_SUPPORT_CONFIG_NAME);
        if(restartSupport != null) {
            RESTART_SUPPORT = Boolean.parseBoolean(restartSupport.getValue());
        }

        Configuration specialLogic = ConfigurationUtils.getConfiguration(configurations, OFTP2MessagingHandler.SPECIAL_LOGIC_CONFIG_NAME);
        if(specialLogic != null) {
            SPECIAL_LOGIC = Boolean.parseBoolean(specialLogic.getValue());
        }
    }
}
