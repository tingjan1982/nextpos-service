package io.nextpos.shared.exception;

public class ObjectAlreadyExistsException extends RuntimeException {

    public ObjectAlreadyExistsException(String id, Class<?> dataObject) {
        super(String.format("Object [%s] already exists: %s", dataObject.getSimpleName(), id));
    }
}
