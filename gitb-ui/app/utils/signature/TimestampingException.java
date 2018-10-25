package utils.signature;

/**
 * Exception to signal an issue with obtaining a signed timestamp from a TSA server.
 */
public class TimestampingException extends Exception {

    public TimestampingException(String message, Throwable cause) {
        super(message, cause);
    }
}
