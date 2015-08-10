package com.gitb.messaging.layer.application.oftp2;

import java.util.Arrays;

public class OFTP2Fields {
    public static final int SSID_COMMAND_IDENTIFIER_POSITION = 0;
    public static final int SSID_COMMAND_IDENTIFIER_LENGTH = 1;
    public static final int SSID_PROTOCOL_LEVEL_POSITION = 1;
    public static final int SSID_PROTOCOL_LEVEL_LENGTH = 1;
    public static final int SSID_USERCODE_FIELD_POSITION = 2;
    public static final int SSID_USERCODE_FIELD_LENGTH   = 25;
    public static final int SSID_PASSWORD_FIELD_POSITION = 27;
    public static final int SSID_PASSWORD_FIELD_LENGTH = 8;
    public static final int SSID_BUFFER_SIZE_FIELD_POSITION = 35;
    public static final int SSID_BUFFER_SIZE_FIELD_LENGTH = 5;
    public static final int SSID_TRANSFER_MODE_FIELD_POSITION = 40;
    public static final int SSID_TRANSFER_MODE_FIELD_LENGTH = 1;
    public static final int SSID_COMPRESSION_SUPPORT_FIELD_POSITION = 41;
    public static final int SSID_COMPRESSION_SUPPORT_FIELD_LENGTH = 1;
    public static final int SSID_RESET_SUPPORT_FIELD_POSITION = 42;
    public static final int SSID_RESET_SUPPORT_FIELD_LENGTH = 1;
    public static final int SSID_SPECIAL_LOGIC_FIELD_POSITION = 43;
    public static final int SSID_SPECIAL_LOGIC_FIELD_LENGTH = 1;
    public static final int SSID_WINDOW_SIZE_FIELD_POSITION = 44;
    public static final int SSID_WINDOW_SIZE_FIELD_LENGTH = 3;
    public static final int SSID_AUTHENTICATION_FIELD_POSITION = 47;
    public static final int SSID_AUTHENTICATION_FIELD_LENGTH = 1;
    public static final int SSID_DRSV1_FIELD_POSITION = 48;
    public static final int SSID_DRSV1_FIELD_LENGTH = 4;
    public static final int SSID_USER_FIELD_POSITION = 52;
    public static final int SSID_USER_FIELD_LENGTH = 8;

    public static final int SFID_COMMAND_IDENTIFIER_POSITION = 0;
    public static final int SFID_COMMAND_IDENTIFIER_LENGTH = 1;
    public static final int SFID_FILE_NAME_POSITION = 1;
    public static final int SFID_FILE_NAME_LENGTH = 26;
    public static final int SFID_DRSV1_FIELD_POSITION = 27;
    public static final int SFID_DRSV1_FIELD_LENGTH = 3;
    public static final int SFID_DATE_FIELD_POSITION = 30;
    public static final int SFID_DATE_FIELD_LENGTH = 8;
    public static final int SFID_TIME_FIELD_POSITION = 38;
    public static final int SFID_TIME_FIELD_LENGTH = 10;
    public static final int SFID_USER_DATA_FIELD_POSITION = 48;
    public static final int SFID_USER_DATA_FIELD_LENGTH = 8;
    public static final int SFID_DESTINATION_FIELD_POSITION = 56;
    public static final int SFID_DESTINATION_FIELD_LENGTH = 25;
    public static final int SFID_ORIGINATOR_FIELD_POSITION = 81;
    public static final int SFID_ORIGINATOR_FIELD_LENGTH = 25;
    public static final int SFID_RECORD_FORMAT_FIELD_POSITION = 106;
    public static final int SFID_RECORD_FORMAT_FIELD_LENGTH = 1;
    public static final int SFID_RECORD_SIZE_FIELD_POSITION = 107;
    public static final int SFID_RECORD_SIZE_FIELD_LENGTH = 5;
    public static final int SFID_FILE_SIZE_FIELD_POSITION = 112;
    public static final int SFID_FILE_SIZE_FIELD_LENGTH = 13;
    public static final int SFID_ORIGINAL_FILE_SIZE_FIELD_POSITION = 125;
    public static final int SFID_ORIGINAL_FILE_SIZE_FIELD_LENGTH = 13;
    public static final int SFID_RESTART_OFFSET_FIELD_POSITION = 138;
    public static final int SFID_RESTART_OFFSET_FIELD_LENGTH = 17;
    public static final int SFID_SECURITY_LEVEL_FIELD_POSITION = 155;
    public static final int SFID_SECURITY_LEVEL_FIELD_LENGTH = 2;
    public static final int SFID_CIPHER_SUITE_FIELD_POSITION = 157;
    public static final int SFID_CIPHER_SUITE_FIELD_LENGTH = 2;
    public static final int SFID_COMPRESSION_ALGORITHM_FIELD_POSITION = 159;
    public static final int SFID_COMPRESSION_ALGORITHM_FIELD_LENGTH = 1;
    public static final int SFID_ENVELOPING_FORMAT_FIELD_POSITION = 160;
    public static final int SFID_ENVELOPING_FORMAT_FIELD_LENGTH = 1;
    public static final int SFID_SIGNED_ACK_FIELD_POSITION = 161;
    public static final int SFID_SIGNED_ACK_FIELD_LENGTH = 1;
    public static final int SFID_FILE_DESCRIPTION_LENGTH_FIELD_POSITION = 162;
    public static final int SFID_FILE_DESCRIPTION_LENGTH_FIELD_LENGTH = 3;
    public static final int SFID_FILE_DESCRIPTION_FIELD_POSITION = 165;

    public static final int SFPA_COMMAND_IDENTIFIER_POSITION = 0;
    public static final int SFPA_COMMAND_IDENTIFIER_LENGTH = 1;
    public static final int SFPA_ANSWER_COUNT_POSITION = 1;
    public static final int SFPA_ANSWER_COUNT_LENGTH = 17;

    public static final int EFID_COMMAND_IDENTIFIER_POSITION = 0;
    public static final int EFID_COMMAND_IDENTIFIER_LENGTH = 1;
    public static final int EFID_RECORD_COUNT_POSITION = 1;
    public static final int EFID_RECORD_COUNT_LENGTH = 17;
    public static final int EFID_UNIT_COUNT_POSITION = 18;
    public static final int EFID_UNIT_COUNT_LENGTH = 17;

    public static final int EFPA_COMMAND_IDENTIFIER_POSITION = 0;
    public static final int EFPA_COMMAND_IDENTIFIER_LENGTH = 1;
    public static final int EFPA_CHANGE_DIRECTION_POSITION = 1;
    public static final int EFPA_CHANGE_DIRECTION_LENGTH = 1;

    public static final int CD_COMMAND_IDENTIFIER_POSITION = 0;
    public static final int CD_COMMAND_IDENTIFIER_LENGTH = 1;

    public static final int ESID_COMMAND_IDENTIFIER_POSITION = 0;
    public static final int ESID_COMMAND_IDENTIFIER_LENGTH = 1;
    public static final int ESID_REASON_CODE_POSITION = 1;
    public static final int ESID_REASON_CODE_LENGTH = 2;
    public static final int ESID_REASON_TEXT_LENGTH_POSITION = 3;
    public static final int ESID_REASON_TEXT_LENGTH_LENGTH = 3;

    public static char getCommandIdentifierFromSSID(byte[] ssid) {
        return (char) ssid[SSID_COMMAND_IDENTIFIER_POSITION];
    }

    public static char getProtocolLevelFromSSID(byte[] ssid) {
        return (char) ssid[SSID_PROTOCOL_LEVEL_POSITION];
    }

    public static String getUserCodeFromSSID(byte[] ssid) {
        return new String(Arrays.copyOfRange(ssid, SSID_USERCODE_FIELD_POSITION,
                SSID_USERCODE_FIELD_POSITION + SSID_USERCODE_FIELD_LENGTH));
    }

    public static String getPasswordFromSSID(byte[] ssid) {
        return new String(Arrays.copyOfRange(ssid, SSID_PASSWORD_FIELD_POSITION,
                SSID_PASSWORD_FIELD_POSITION + SSID_PASSWORD_FIELD_LENGTH));
    }

    public static String getBufferSizeFromSSID(byte[] ssid) {
        return new String(Arrays.copyOfRange(ssid, SSID_BUFFER_SIZE_FIELD_POSITION,
                SSID_BUFFER_SIZE_FIELD_POSITION + SSID_BUFFER_SIZE_FIELD_LENGTH));
    }

    public static String getWindowSizeFromSSID(byte[] ssid) {
        return new String(Arrays.copyOfRange(ssid, SSID_WINDOW_SIZE_FIELD_POSITION,
                SSID_WINDOW_SIZE_FIELD_POSITION + SSID_WINDOW_SIZE_FIELD_LENGTH));
    }

    public static String getTransferModeFromSSID(byte[] ssid) {
        return new String(Arrays.copyOfRange(ssid, SSID_TRANSFER_MODE_FIELD_POSITION,
                SSID_TRANSFER_MODE_FIELD_POSITION + SSID_TRANSFER_MODE_FIELD_LENGTH));
    }

    public static String getCompressionSupportFromSSID(byte[] ssid) {
        return new String(Arrays.copyOfRange(ssid, SSID_COMPRESSION_SUPPORT_FIELD_POSITION,
                SSID_COMPRESSION_SUPPORT_FIELD_POSITION + SSID_COMPRESSION_SUPPORT_FIELD_LENGTH));
    }

    public static String getResetSupportFromSSID(byte[] ssid) {
        return new String(Arrays.copyOfRange(ssid, SSID_RESET_SUPPORT_FIELD_POSITION,
                SSID_RESET_SUPPORT_FIELD_POSITION + SSID_RESET_SUPPORT_FIELD_LENGTH));
    }

    public static String getSpecialLogicFromSSID(byte[] ssid) {
        return new String(Arrays.copyOfRange(ssid, SSID_SPECIAL_LOGIC_FIELD_POSITION,
                SSID_SPECIAL_LOGIC_FIELD_POSITION + SSID_SPECIAL_LOGIC_FIELD_LENGTH));
    }

    public static String getAuthenticationFieldFromSSID(byte[] ssid) {
        return new String(Arrays.copyOfRange(ssid, SSID_AUTHENTICATION_FIELD_POSITION,
                SSID_AUTHENTICATION_FIELD_LENGTH + SSID_AUTHENTICATION_FIELD_POSITION));
    }

    public static int getSSIDSize() {
        return  SSID_USER_FIELD_POSITION + SSID_USER_FIELD_LENGTH + 1;
    }

    public static String getFileNameFromSfid(byte[] sfid) {
        return new String(Arrays.copyOfRange(sfid, SFID_FILE_NAME_POSITION,
                SFID_FILE_NAME_POSITION + SFID_FILE_NAME_LENGTH));
    }

    public static String getFileDescriptionLengthFromSfid(byte[] sfid) {
        return new String(Arrays.copyOfRange(sfid, SFID_FILE_DESCRIPTION_LENGTH_FIELD_POSITION,
                SFID_FILE_DESCRIPTION_LENGTH_FIELD_LENGTH + SFID_FILE_DESCRIPTION_LENGTH_FIELD_POSITION));
    }

    public static String getRestartOffsetFromSfid(byte[] sfid) {
        return new String(Arrays.copyOfRange(sfid, SFID_RESTART_OFFSET_FIELD_POSITION,
                SFID_RESTART_OFFSET_FIELD_LENGTH + SFID_RESTART_OFFSET_FIELD_POSITION));
    }

    public static String getReasonTextLengthFromEsid(byte[] esid) {
        return new String(Arrays.copyOfRange(esid, ESID_REASON_TEXT_LENGTH_POSITION,
                ESID_REASON_TEXT_LENGTH_LENGTH + ESID_REASON_TEXT_LENGTH_POSITION));
    }

    public static int getSFIDSize() {
        return  SFID_FILE_DESCRIPTION_LENGTH_FIELD_POSITION + SFID_FILE_DESCRIPTION_LENGTH_FIELD_LENGTH;
    }

    public static String getAnswerCountFromSFPA(byte[] sfpa) {
        return new String(Arrays.copyOfRange(sfpa, SFPA_ANSWER_COUNT_POSITION,
                SFPA_ANSWER_COUNT_LENGTH + SFPA_ANSWER_COUNT_POSITION));
    }

    public static int getSFPASize() {
        return SFPA_ANSWER_COUNT_POSITION + SFPA_ANSWER_COUNT_LENGTH;
    }

    public static int getEFIDSize() {
        return  EFID_UNIT_COUNT_POSITION + EFID_UNIT_COUNT_LENGTH;
    }

    public static int getEFPASize() {
        return  EFPA_CHANGE_DIRECTION_POSITION + EFPA_CHANGE_DIRECTION_LENGTH;
    }

    public static int getCDSize() {
        return  CD_COMMAND_IDENTIFIER_POSITION + CD_COMMAND_IDENTIFIER_LENGTH;
    }

    public static int getESIDSize() {
        return ESID_REASON_TEXT_LENGTH_POSITION + ESID_REASON_TEXT_LENGTH_LENGTH;
    }
}
