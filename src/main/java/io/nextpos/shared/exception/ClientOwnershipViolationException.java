package io.nextpos.shared.exception;

import io.nextpos.client.data.Client;

public class ClientOwnershipViolationException extends RuntimeException {

    public ClientOwnershipViolationException(final String message) {
        super(message);
    }

    public ClientOwnershipViolationException(final Object clientObject, Client client) {
        super(String.format("Client object %s is not owned by %s", clientObject.getClass(), client.getClientName()));
    }


}
