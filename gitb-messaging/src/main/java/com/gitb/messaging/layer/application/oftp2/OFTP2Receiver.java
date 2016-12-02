package com.gitb.messaging.layer.application.oftp2;

import com.gitb.core.Configuration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.messaging.SecurityUtils;
import com.gitb.messaging.layer.AbstractTransactionReceiver;
import com.gitb.messaging.layer.transport.tcp.TCPMessagingHandler;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.types.StringType;
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
import java.util.List;

public class OFTP2Receiver extends AbstractTransactionReceiver{

    private static final Logger logger = LoggerFactory.getLogger(OFTP2Receiver.class);

    public static boolean SECURE_CONNECTION = true;
    public static int BUFFER_SIZE = 4096;
    public static int WINDOW_SIZE = 64;
    public static boolean COMPRESSION_SUPPORT = false;
    public static boolean RESTART_SUPPORT = false;
    public static boolean SPECIAL_LOGIC = false;

    public OFTP2Receiver(SessionContext session, TransactionContext transaction) throws IOException {
        super(session, transaction);
    }

    public Message receive(List<Configuration> configurations, Message inputs) throws Exception {
        waitUntilMessageReceived();

        logger.debug("Connection established: " + socket);

        parseConfigurations(configurations);

        Socket socket = getSocket();

        if(SECURE_CONNECTION) {
            socket = SecurityUtils.secureSocket(transaction, socket);
            ((SSLSocket) socket).setUseClientMode(false);
        }

        InputStream inputStream   = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();
        byte[] buffer;

        //send SSRM (Start Session Ready Message)
        byte[] ssrm  = createSSRMMessage();
        TCPMessagingHandler.sendBytes(outputStream, OFTP2MessagingHandler.encodeMessage(ssrm));
        logger.debug("SSRM (Start Session Ready Message) sent...");

        //receive SSID (Start Session)
        byte[] ssid = OFTP2MessagingHandler.readOFTPMessage(inputStream);
        logger.debug("SSID (Start Session) received...");

        //validate SSID (Start Session) and authenticate user credentials
        OFTP2MessagingHandler.validateSSID(ssid);
        OFTP2MessagingHandler.authenticate(transaction.getSelf().getConfig(), ssid);
        logger.debug("Valid SSID message and user credentials...");

        //send SSID (Start Session)
        byte[] responseSsid = OFTP2MessagingHandler.createSSIDMessage(ssid, BUFFER_SIZE, COMPRESSION_SUPPORT,
                RESTART_SUPPORT, SPECIAL_LOGIC, WINDOW_SIZE);
        TCPMessagingHandler.sendBytes(outputStream, OFTP2MessagingHandler.encodeMessage(responseSsid));
        logger.debug("SSID (Start Session) sent...");

        //receive SFID (Start File)
        byte[] sfid = OFTP2MessagingHandler.readOFTPMessage(inputStream);
        OFTP2MessagingHandler.validateSFID(sfid);
        logger.debug("SFID (Start File) received...");

        //send SFPA (Star File Positive Answer)
        int restartOffset = Integer.parseInt(OFTP2Fields.getRestartOffsetFromSfid(sfid));
        int answerCount = RESTART_SUPPORT ? Math.min(restartOffset, 0) : 0;
        byte[] sfpa = OFTP2MessagingHandler.createSFPAMessage(answerCount);
        TCPMessagingHandler.sendBytes(outputStream, OFTP2MessagingHandler.encodeMessage(sfpa));
        logger.debug("SFPA (Start File Positive Answer) sent...");

        //receive DATA & EFID
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        byte[] data;
        byte[] efid;

        while(true) {
            buffer = OFTP2MessagingHandler.readOFTPMessage(inputStream);

            if(buffer != null && buffer.length > 0) {
                if (buffer[0] == OFTP2Constants.DATA){
                    byte[] normalizedData = extractDataFromSubrecords(buffer);
                    dataStream.write(normalizedData);
                    logger.debug("DATA received...");
                }
                else if(buffer[0] == OFTP2Constants.EFID) {
                    efid = buffer;
                    logger.debug("EFID (End File) received...");
                    break;
                }
                else {
                    throw new GITBEngineInternalError("Unexpected command identifier. One of ["+
                            OFTP2Constants.DATA+", or [" + OFTP2Constants.EFID +"] is expected.");
                }
            } else {
                throw new GITBEngineInternalError("Expected file data, but nothing received...");
            }
        }

        data = dataStream.toByteArray();
        byte[] receivedContent = data;

        //send EFPA
        byte[] efpa = OFTP2MessagingHandler.createEFPAMessage();
        TCPMessagingHandler.sendBytes(outputStream, OFTP2MessagingHandler.encodeMessage(efpa));
        logger.debug("EFPA (End File Positive Answer) sent...");

        //receive CD (Change Direction)
        byte[] cd = OFTP2MessagingHandler.readOFTPMessage(inputStream);
        logger.debug("CD (Change Direction) received...");
        //do nothing

        //send ESID (End Session)
        byte[] esid = OFTP2MessagingHandler.createESIDMessage();
        TCPMessagingHandler.sendBytes(outputStream, OFTP2MessagingHandler.encodeMessage(esid));
        logger.debug("ESID (End Session) sent...");

        //create outputs
        Message message = new Message();

        BinaryType fileContent = new BinaryType();
        fileContent.setValue(receivedContent);

        StringType fileName = new StringType();
        fileName.setValue(OFTP2Fields.getFileNameFromSfid(sfid).trim());

        message.getFragments().put(OFTP2MessagingHandler.FILE_CONTENT_FIELD_NAME, fileContent);
        message.getFragments().put(OFTP2MessagingHandler.FILE_NAME_FIELD_NAME, fileName);

        return message;
    }

    private byte[] createSSRMMessage() {
        String ssrm = "";
        ssrm += OFTP2Constants.SSRM;
        ssrm += "ODETTE FTP READY"; //message
        ssrm += OFTP2Constants.CARRIAGE_RETURN;
        return ssrm.getBytes();
    }

    private byte[] extractDataFromSubrecords(byte[] data) throws IOException {
        int index = 1;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        //iterate over subrecords, and populate them into a byte array
        while(index < data.length) {
            byte headerOctet = data[index];

            //get subrecord details
            boolean compressed  = (headerOctet & 0x40) == 0x40;
            boolean endOfRecord = (headerOctet & 0x80) == 0x80;
            byte count = (byte) (headerOctet & 0x3f);

            // read the subrecord
            int start = index + 1;
            int end   = start + count;

            byte[] subrecord = Arrays.copyOfRange(data, start, end);
            stream.write(subrecord);
            index = end;
        }

        return stream.toByteArray();
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
