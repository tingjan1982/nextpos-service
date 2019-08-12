package io.nextpos.shared.model;

import io.nextpos.product.data.Version;

import java.io.Serializable;
import java.util.Optional;

/**
 * This should be implemented by any business object that wants to support
 * object versioning. There are two version: DESIGN and LIVE.
 *
 * It will also have a deploy() method that is intended to move
 * DESIGN version to LIVE, and copy a new DESIGN version.
 *
 * @param <ID>
 * @param <T>
 */
public interface ParentObject<ID extends Serializable, T extends ObjectVersioning> extends ClientObject {

    ID getId();

    T getDesignVersion();

    T getLiveVersion();

    Optional<T> getObjectByVersion(Version version);

    void deploy();
}
