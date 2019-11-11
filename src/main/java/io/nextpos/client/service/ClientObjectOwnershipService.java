package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.ClientObject;
import io.nextpos.shared.model.WithClientId;

import java.util.function.Supplier;

public interface ClientObjectOwnershipService {

    <T extends ClientObject> T checkOwnership(Client belongToClient, Supplier<T> clientObjectProvider);

    <T extends WithClientId> T checkWithClientIdOwnership(Client belongToClient, Supplier<T> objectProvider);
}
