package io.nextpos.shared.exception;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.ClientObject;

public class ClientOwnershipViolationException extends RuntimeException {

    public ClientOwnershipViolationException(final ClientObject clientObject, Client client) {
        super(String.format("Client object %s is not owned by %s", clientObject.getClass(), client.getClientName()));
    }
}
