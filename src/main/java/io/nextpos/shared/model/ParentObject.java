package io.nextpos.shared.model;

import io.nextpos.product.data.Version;
import io.nextpos.shared.exception.ObjectNotFoundException;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
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

    default T getObjectByVersionThrows(Version version) {
        return this.getObjectByVersion(version).orElseThrow(() -> {
            throw new ObjectNotFoundException(getId() + "-" + version, getObjectVersioningClassType());
        });
    }

    /**
     * How to resolve generic class type at runtime:
     * 
     * https://www.javacodegeeks.com/2013/12/advanced-java-generics-retreiving-generic-type-arguments.html
     */
    default Class<T> getObjectVersioningClassType() {
        ParameterizedType parameterizedType = (ParameterizedType) this.getClass().getGenericInterfaces()[0];
        return (Class<T>) parameterizedType.getActualTypeArguments()[1];
    }

    void deploy();
}
