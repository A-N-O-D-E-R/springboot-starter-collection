package bio.anode.sila.exception;

public class SilaCommandException extends RuntimeException {

    public SilaCommandException(String message) {
        super(message);
    }

    public SilaCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
