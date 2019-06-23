package io.nextpos.shared.exception;

public class ObjectNotFoundException extends RuntimeException {

    public ObjectNotFoundException(String id, Class<?> dataObject) {
        super(String.format("Object [%s] not found: %s", dataObject.getSimpleName(), id));
    }
}
