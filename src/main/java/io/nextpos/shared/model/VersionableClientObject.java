package io.nextpos.shared.model;

import io.nextpos.client.data.Client;

public interface VersionableClientObject<T> {

    Client getClient();

    void setClient(Client client);

    T getLatestVersion();

    T addNewVersion(T object);

}
