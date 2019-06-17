package io.nextpos.shared.model;

public interface VersionableClientObject<T> {

    Client getClient();

    void setClient(Client client);

    T getLatestVersion();

    T addNewVersion(T object);

    enum ObjectState {

        DESIGN, DEPLOYED, RETIRED
    }
}
