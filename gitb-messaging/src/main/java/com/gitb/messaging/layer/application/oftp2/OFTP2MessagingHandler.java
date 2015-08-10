package com.gitb.messaging.layer.application.oftp2;

import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.SecurityUtils;
import com.gitb.messaging.SessionManager;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.model.tcp.ITransactionListener;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.messaging.model.tcp.ITransactionSender;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import com.gitb.utils.ConfigurationUtils;
import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.kohsuke.MetaInfServices;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@MetaInfServices(IMessagingHandler.class)
public class OFTP2MessagingHandler extends AbstractMessagingHandler {

    public static final String FILE_CONTENT_FIELD_NAME = "file_content";
    public static final String FILE_NAME_FIELD_NAME = "file_name";

    public static final String USERCODE_CONFIG_NAME = "user.code";
    public static final String PASSWORD_CONFIG_NAME = "user.pass";
    public static final String SECURE_CONNECTION_CONFIG_NAME = "secure.connection";
    public static final String BUFFER_SIZE_CONFIG_NAME = "buffer.size";
    public static final String WINDOW_SIZE_CONFIG_NAME = "window.size";
    public static final String COMPRESSION_SUPPORT_CONFIG_NAME = "compression.support";
    public static final String RESTART_SUPPORT_CONFIG_NAME = "restart.support";
    public static final String SPECIAL_LOGIC_CONFIG_NAME = "special.logic";

    public static int TICKER_COUNTER = 1;

    private static final String MODULE_DEFINITION_XML = "/oftp2-messaging-definition.xml";

    private static MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

    @Override
    public MessagingModule getModuleDefinition() {
        return module;
    }

    @Override
    public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) throws IOException {
        return new OFTP2Receiver(sessionContext, transactionContext);
    }

    @Override
    public ITransactionSender getSender(SessionContext sessionContext, TransactionContext transactionContext) {
        return new OFTP2Sender(sessionContext, transactionContext);
    }

    @Override
    public ITransactionListener getListener(SessionContext sessionContext, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        return new OFTP2Listener(sessionContext, receiverTransactionContext, senderTransactionContext);
    }

    @Override
    public void beginTransaction(String sessionId, String transactionId, String from, String to, List<Configuration> configurations) {
        super.beginTransaction(sessionId, transactionId, from, to, configurations);

        SessionContext sessionContext = SessionManager.getInstance().getSession(sessionId);
        List<TransactionContext> transactions = sessionContext.getTransactions(transactionId);

        //create an SSLContext and save it to the transaction context
        SSLContext sslContext = SecurityUtils.createSSLContext();

        for(TransactionContext transactionContext : transactions) {
            transactionContext.setParameter(SSLContext.class, sslContext);
        }
    }

    public static byte[] createSSIDMessage(String usercode, String password, int bufferSize, char transferMode,
                        boolean compressionSupport, boolean restartSupport, boolean specialLogic, int windowSize,
                        boolean authentication, String drsv1, String userData) {

        String ssid = "";
        ssid += OFTP2Constants.SSID;
        ssid += OFTP2Constants.OFTP_PROTOCOL_LEVEL;
        ssid += appendEmptyString(usercode, OFTP2Fields.SSID_USERCODE_FIELD_LENGTH);
        ssid += appendEmptyString(password, OFTP2Fields.SSID_PASSWORD_FIELD_LENGTH);
        ssid += prependZeros(""+bufferSize, OFTP2Fields.SSID_BUFFER_SIZE_FIELD_LENGTH);
        ssid += transferMode;
        ssid += yesNo(compressionSupport);
        ssid += yesNo(restartSupport);
        ssid += yesNo(specialLogic);
        ssid += prependZeros(""+windowSize, OFTP2Fields.SSID_WINDOW_SIZE_FIELD_LENGTH);
        ssid += yesNo(authentication);
        ssid += appendEmptyString(drsv1, OFTP2Fields.SSID_DRSV1_FIELD_LENGTH);
        ssid += appendEmptyString(userData, OFTP2Fields.SSID_USER_FIELD_LENGTH);
        ssid += OFTP2Constants.CARRIAGE_RETURN;
        return ssid.getBytes();
    }

    public static byte[] createSSIDMessage(byte[] ssid, int _bufferSize, boolean _compressionSupport,
                                           boolean _restartSupport, boolean _specialLogic, int _windowSize) {
        String usercode = OFTP2Fields.getUserCodeFromSSID(ssid);
        String password = OFTP2Fields.getPasswordFromSSID(ssid);
        int bufferSize  = Math.min(_bufferSize, Integer.parseInt(OFTP2Fields.getBufferSizeFromSSID(ssid)));

        //get transfer mode and reverse it
        char transferMode = OFTP2Constants.BOTH_RECEIVER_SENDER_TRANSFER_MODE;
        char receivedTransferMode = OFTP2Fields.getTransferModeFromSSID(ssid).charAt(0);
        if(receivedTransferMode == OFTP2Constants.RECEIVER_ONLY_TRANSFER_MODE) {
            transferMode = OFTP2Constants.SENDER_ONLY_TRANSFER_MODE;
        } else if(receivedTransferMode == OFTP2Constants.SENDER_ONLY_TRANSFER_MODE) {
            transferMode = OFTP2Constants.RECEIVER_ONLY_TRANSFER_MODE;
        }

        boolean compressionSupport = _compressionSupport && valueOfYesNo(OFTP2Fields.getCompressionSupportFromSSID(ssid));
        boolean restartSupport = _restartSupport && valueOfYesNo(OFTP2Fields.getResetSupportFromSSID(ssid));
        boolean specialLogic = _specialLogic && valueOfYesNo(OFTP2Fields.getSpecialLogicFromSSID(ssid));
        int windowSize = Math.min(_windowSize, Integer.parseInt(OFTP2Fields.getWindowSizeFromSSID(ssid)));

        return createSSIDMessage(usercode, password, bufferSize, transferMode, compressionSupport, restartSupport, specialLogic, windowSize, false, null, null);
    }

    public static byte[] createSFIDMessage(String fileName, Date fileDateTime, String destination, String originator, long fileSize) {
        String sfid = "";
        sfid += OFTP2Constants.SFID;
        sfid += appendEmptyString(fileName, OFTP2Fields.SFID_FILE_NAME_LENGTH);
        sfid += appendEmptyString(null, OFTP2Fields.SFID_DRSV1_FIELD_LENGTH);
        sfid += new SimpleDateFormat(OFTP2Constants.DATE_FORMAT).format(fileDateTime);
        sfid += new SimpleDateFormat(OFTP2Constants.TIME_FORMAT).format(fileDateTime) + prependZeros("" + TICKER_COUNTER, 4);
        sfid += appendEmptyString(null, OFTP2Fields.SFID_USER_DATA_FIELD_LENGTH); //user data is always null
        sfid += appendEmptyString(destination, OFTP2Fields.SFID_DESTINATION_FIELD_LENGTH);
        sfid += appendEmptyString(originator, OFTP2Fields.SFID_ORIGINATOR_FIELD_LENGTH);
        sfid += "U"; //record format: U for Unstructured
        sfid += prependZeros("0", OFTP2Fields.SFID_RECORD_SIZE_FIELD_LENGTH); //record size: 0 for Unstructured Files
        sfid += prependZeros(""+fileSize, OFTP2Fields.SFID_FILE_SIZE_FIELD_LENGTH); //file size: 1
        sfid += prependZeros(""+fileSize, OFTP2Fields.SFID_ORIGINAL_FILE_SIZE_FIELD_LENGTH); //original file size: 1
        sfid += prependZeros("0", OFTP2Fields.SFID_RESTART_OFFSET_FIELD_LENGTH); //restart offset: 0
        sfid += prependZeros("0", OFTP2Fields.SFID_SECURITY_LEVEL_FIELD_LENGTH); //security code: 0 for NO SECURITY SERVICES
        sfid += prependZeros("0", OFTP2Fields.SFID_CIPHER_SUITE_FIELD_LENGTH); //cipher suite: 0 for NO CIPHER SUITE SELECTION
        sfid += prependZeros("0", OFTP2Fields.SFID_COMPRESSION_ALGORITHM_FIELD_LENGTH); //compression algorithm: 0 for NO COMPRESSION
        sfid += prependZeros("0", OFTP2Fields.SFID_ENVELOPING_FORMAT_FIELD_LENGTH); //enveloping format: 0 for NO ENVELOPING
        sfid += yesNo(false); //signed ack: false
        sfid += prependZeros("0", OFTP2Fields.SFID_FILE_DESCRIPTION_LENGTH_FIELD_LENGTH);
        return sfid.getBytes();
    }

    public static byte[] createSFPAMessage(int answerCount){
        String sfpa = "";
        sfpa += OFTP2Constants.SFPA;
        sfpa += prependZeros(""+answerCount, OFTP2Fields.SFPA_ANSWER_COUNT_LENGTH);
        return sfpa.getBytes();
    }

    public static byte[] createEFIDMessage(int recordCount, int unitCount) {
        String efid = "";
        efid += OFTP2Constants.EFID;
        efid += prependZeros("" + recordCount, OFTP2Fields.EFID_RECORD_COUNT_LENGTH);
        efid += prependZeros("" + unitCount, OFTP2Fields.EFID_UNIT_COUNT_LENGTH);
        return efid.getBytes();
    }

    public static byte[] createEFPAMessage() {
        String efpa = "";
        efpa += OFTP2Constants.EFPA;
        efpa += yesNo(false); //change direction is false
        return efpa.getBytes();
    }

    public static byte[] createESIDMessage() {
        String esid = "";
        esid += OFTP2Constants.ESID;
        esid += prependZeros("0", OFTP2Fields.ESID_REASON_CODE_LENGTH); //means normal termination
        esid += prependZeros("0", OFTP2Fields.ESID_REASON_TEXT_LENGTH_LENGTH); //0: reason text length
        esid += OFTP2Constants.CARRIAGE_RETURN;
        return esid.getBytes();
    }

    public static byte[] createCDMessage() {
        String cd = "";
        cd += OFTP2Constants.CD;
        return cd.getBytes();
    }

    public static void validateSSID(byte[] ssid){
        int length = ssid.length;
        int ssidSize = OFTP2Fields.getSSIDSize();
        if(length != ssidSize) {
            throw new GITBEngineInternalError("Invalid SSID size [" + length + "], " +
                    "it must be ["+ ssidSize+"]");
        }

        validateCommandIdentifier(ssid, OFTP2Constants.SSID);

        char oftpVersion = OFTP2Fields.getProtocolLevelFromSSID(ssid);
        if(oftpVersion != OFTP2Constants.OFTP_PROTOCOL_LEVEL) {
            throw new GITBEngineInternalError("Wrong OFTP version, " +
                    "it must be ["+ OFTP2Constants.OFTP_PROTOCOL_LEVEL +"] for OFTP 2.0 according to RFC 5024 specification");
        }
    }

    public static void validateSFID(byte[] sfid){
        int length = sfid.length;
        int sfidSize = OFTP2Fields.getSFIDSize();
        if(length < sfidSize) {
            throw new GITBEngineInternalError("Invalid SFID size [" + length + "], " +
                    "it must be at least ["+ sfidSize+"]");
        }

        validateCommandIdentifier(sfid, OFTP2Constants.SFID);
    }

    public static void validateCommandIdentifier(byte[] message, char commandIdentifier) {
        char identifier = (char) message[0];
        if(identifier != commandIdentifier){
            throw new GITBEngineInternalError("Unexpected command identifier ["+identifier+"], " +
                    "it should have been ["+ commandIdentifier + "]");
        }
    }

    public static void authenticate(List<Configuration>configurations, byte[] ssid) {
        String usercode = OFTP2Fields.getUserCodeFromSSID(ssid).trim();
        String password = OFTP2Fields.getPasswordFromSSID(ssid).trim();

        Configuration _usercode = ConfigurationUtils.getConfiguration(configurations, USERCODE_CONFIG_NAME);
        Configuration _password = ConfigurationUtils.getConfiguration(configurations, PASSWORD_CONFIG_NAME);

        if(!_usercode.getValue().contentEquals(usercode) || !_password.getValue().contentEquals(password)){
            throw new GITBEngineInternalError("Authentication Failure: Incorrect user code or password.");
        }
    }

    public static byte[] readOFTPMessage(InputStream inputStream) throws IOException {
        //read header
        byte[] header = new byte[OFTP2Constants.STB_HEADER_SIZE];
        inputStream.read(header);

        BigEndianHeapChannelBuffer buffer = new BigEndianHeapChannelBuffer(header);
        byte versionAndFlags = buffer.readByte();
        if ((versionAndFlags & OFTP2Constants.STB_V1_NOFLAGS_HEADER) != OFTP2Constants.STB_V1_NOFLAGS_HEADER) {
            throw new GITBEngineInternalError("Format error. Invalid STB header version: " +
                    ((int) (versionAndFlags >> 4)));
        }

        // Read the length field.
        int messageLength = (buffer.readMedium() - OFTP2Constants.STB_HEADER_SIZE);
        byte[] message = new byte[messageLength];
        inputStream.read(message);
        return message;
    }

    public static byte[] encodeMessage(byte[] message) {
        BigEndianHeapChannelBuffer header = new BigEndianHeapChannelBuffer(OFTP2Constants.STB_HEADER_SIZE);
        header.writeByte(OFTP2Constants.STB_V1_NOFLAGS_HEADER);
        header.writeMedium(OFTP2Constants.STB_HEADER_SIZE + message.length);

        BigEndianHeapChannelBuffer body   = new BigEndianHeapChannelBuffer(message);

        return ChannelBuffers.wrappedBuffer(header, body).toByteBuffer().array();
    }

    public static byte[] decodeMessage(byte[] message) {
        BigEndianHeapChannelBuffer buffer = new BigEndianHeapChannelBuffer(message);

        // Make sure if the length field was received.
        if (buffer.readableBytes() < OFTP2Constants.STB_MIN_BUFFER_SIZE) {
            // The length field was not received yet - return null.
            // This method will be invoked again when more packets are
            // received and appended to the buffer.
            return null;
        }

        // Mark the current buffer position before reading the length field
        // because the whole frame might not be in the buffer yet.
        // We will reset the buffer position to the marked position if
        // there's not enough bytes in the buffer.
        buffer.markReaderIndex();

        // check STB version avoiding flags
        byte versionAndFlags = buffer.readByte();
        if ((versionAndFlags & OFTP2Constants.STB_V1_NOFLAGS_HEADER) != OFTP2Constants.STB_V1_NOFLAGS_HEADER) {
            throw new GITBEngineInternalError("Format error. Invalid STB header version: " +
                    ((int) (versionAndFlags >> 4)));
        }

        // Read the length field.
        int length = (buffer.readMedium() - OFTP2Constants.STB_HEADER_SIZE);

        if (buffer.readableBytes() < length) {
            buffer.resetReaderIndex();
            return null;
        }

        // There's enough bytes in the buffer. Read it.
        ChannelBuffer frame = buffer.readBytes(length);

        // Successfully decoded a frame. Return the decoded frame.
        return frame.array();
    }

    private static String appendEmptyString(String s, int fieldLength) {
        String returnString = s;

        if(returnString == null) {
            returnString = "";
        }

        int difference = fieldLength - returnString.length();
        if(difference > 0) {
            byte[] appended = new byte[difference];
            Arrays.fill(appended, (byte) ' ');
            returnString = returnString + new String(appended);
        }

        return returnString;
    }

    private static String prependZeros(String s, int fieldLength) {
        String returnString = s;

        if(returnString == null) {
            returnString = "";
        }

        int difference = fieldLength - returnString.length();
        if(difference > 0) {
            byte[] appended = new byte[difference];
            Arrays.fill(appended, (byte) '0');
            returnString = new String(appended) + returnString;
        }

        return returnString;
    }

    public static long computeVirtualFileSize(long unitCount) {

        // avoid dividing zero or division by zero
        if (unitCount == 0)
            return 0;

        long fileSize = (unitCount / OFTP2Constants.DEFAULT_RECORD_SIZE);
        if ((unitCount % OFTP2Constants.DEFAULT_RECORD_SIZE) > 0) {
            fileSize++;
        }

        return fileSize;
    }

    private static String yesNo(boolean set) {
        return (set ? "Y" : "N");
    }

    private static final boolean valueOfYesNo(String parameter) {
        return ("Y".equals(parameter) ? true : false);
    }
}
