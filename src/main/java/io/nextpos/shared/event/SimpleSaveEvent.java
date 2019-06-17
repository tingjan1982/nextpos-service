package io.nextpos.shared.event;

import io.nextpos.shared.model.BaseObject;
import org.springframework.context.ApplicationEvent;

public class SimpleSaveEvent extends ApplicationEvent {

    private final BaseObject baseObject;

    public SimpleSaveEvent(final BaseObject baseObject) {
        super(baseObject);

        this.baseObject = baseObject;
    }

    public BaseObject getBaseObject() {
        return baseObject;
    }
}
