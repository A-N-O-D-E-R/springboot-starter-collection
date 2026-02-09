package bio.anode.sila.exception;

public class SilaConnectionException extends RuntimeException {

    public SilaConnectionException(String message) {
        super(message);
    }

    public SilaConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
