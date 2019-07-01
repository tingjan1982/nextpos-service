package io.nextpos.shared.model;

import java.io.Serializable;

public interface ParentObject<T extends Serializable> {

    T getId();
}
