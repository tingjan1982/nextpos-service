package io.nextpos.shared.exception;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(final String errorMsg) {
        super(errorMsg);
    }
}
