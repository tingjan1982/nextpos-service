package io.nextpos.shared.model;

public interface ObjectVersion {

    Integer getVersion();

    void setVersion(Integer version);

    BusinessObjectState getState();

    void setState(BusinessObjectState state);
}
