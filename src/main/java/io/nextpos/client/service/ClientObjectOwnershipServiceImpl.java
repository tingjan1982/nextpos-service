package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.shared.exception.ClientOwnershipViolationException;
import io.nextpos.shared.model.ClientObject;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class ClientObjectOwnershipServiceImpl implements ClientObjectOwnershipService {

    @Override
    public <T extends ClientObject> T checkOwnership(final Client belongToClient, final Supplier<T> clientObjectProvider) {
        final T clientObject = clientObjectProvider.get();

        if (clientObject != null) {
            if (!clientObject.getClient().equals(belongToClient)) {
                throw new ClientOwnershipViolationException(clientObject, belongToClient);
            }
        }

        return clientObject;
    }
}
