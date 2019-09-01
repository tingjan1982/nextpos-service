package io.nextpos.shared.exception;

import io.nextpos.client.data.Client;

public class ClientAccountException extends RuntimeException {

    public ClientAccountException(final String msg, final String clientId) {
        super(msg + ": " + clientId);
    }

    public ClientAccountException(final String msg, final Client client) {
        super(msg + ": " + client.getId());
    }
}
