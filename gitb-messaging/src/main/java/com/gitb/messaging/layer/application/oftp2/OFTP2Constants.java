package com.gitb.messaging.layer.application.oftp2;

public class OFTP2Constants {

    public static final char AUCH = 'A'; //Authentication Challenge
    public static final char AURP = 'S'; //Authentication Response
    public static final char CD   = 'R'; //Change Direction
    public static final char CDT  = 'C'; //Set Credit
    public static final char DATA = 'D'; //Data
    public static final char EERP = 'E'; //End to End Response
    public static final char EFID = 'T'; //End File
    public static final char EFNA = '5'; //End File Negative Answer
    public static final char EFPA = '4'; //End File Positive Answer
    public static final char ESID = 'F'; //End Session
    public static final char NERP = 'N'; //Negative End to End Response
    public static final char RTR  = 'P'; //Ready To Receive
    public static final char SECD = 'J'; //Security Change Direction
    public static final char SFID = 'H'; //Start File
    public static final char SFNA = '3'; //Start File Negative Answer
    public static final char SFPA = '2'; //Start File Positive Answer
    public static final char SSID = 'X'; //Start Session
    public static final char SSRM = 'I'; //Start Session Ready Message

    public static final int  STB_HEADER_SIZE = 4;
    public static final int  STB_MIN_BUFFER_SIZE = 5;
    public static final byte STB_V1_NOFLAGS_HEADER = (byte) 0x10;

    public static final char OFTP_PROTOCOL_LEVEL = '5'; //ODETTE FTP version 2.0 as specified in RFC 5024

    public static final char RECEIVER_ONLY_TRANSFER_MODE = 'R';
    public static final char SENDER_ONLY_TRANSFER_MODE = 'S';
    public static final char BOTH_RECEIVER_SENDER_TRANSFER_MODE = 'B';

    public static final char CARRIAGE_RETURN = '\r';
    public static final char NEW_LINE = '\n';

    public static final String DATE_FORMAT = "yyyyMMdd";
    public static final String TIME_FORMAT = "HHmmss";

    public static final int MAX_SUBRECORD_HEADER_SIZE = 63;
    public static final int DEFAULT_RECORD_SIZE = 1024;
    
}
