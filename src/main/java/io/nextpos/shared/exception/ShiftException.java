package io.nextpos.shared.exception;

public class ShiftException extends GeneralApplicationException {

    public ShiftException(final String clientId) {
        super(String.format("There is no active shift for client[%s]. Please open shift first.", clientId));
    }
}
