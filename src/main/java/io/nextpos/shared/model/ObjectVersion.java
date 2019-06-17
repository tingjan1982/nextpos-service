package io.nextpos.shared.model;

public interface ObjectVersion {

    Integer getVersion();

    void setVersion(Integer version);

    VersionableClientObject.ObjectState getState();

    void setState(VersionableClientObject.ObjectState state);
}
