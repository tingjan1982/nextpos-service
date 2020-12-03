package io.nextpos.shared.exception;

public class GeneralApplicationException extends RuntimeException {

    public GeneralApplicationException(final String message) {
        super(message);
    }

    public GeneralApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
