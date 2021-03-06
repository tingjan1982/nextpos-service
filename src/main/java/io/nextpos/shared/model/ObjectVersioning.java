package io.nextpos.shared.model;

public interface ObjectVersioning<P extends ParentObject> {

    P getParent();

    int getVersionNumber();
}
