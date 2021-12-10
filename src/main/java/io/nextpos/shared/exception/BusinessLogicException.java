package io.nextpos.shared.exception;

public class BusinessLogicException extends RuntimeException {

    private final String localizedMessageKey;

    public BusinessLogicException(final String message) {
        this(null, message);
    }

    public BusinessLogicException(String localizedMessageKey, final String message) {
        super(message);
        this.localizedMessageKey = localizedMessageKey;
    }

    public String getLocalizedMessageKey() {
        return localizedMessageKey;
    }
}
