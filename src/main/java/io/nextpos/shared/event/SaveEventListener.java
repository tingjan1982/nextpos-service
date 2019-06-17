package io.nextpos.shared.event;

import io.nextpos.shared.model.BaseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SaveEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleSaveEvent(SimpleSaveEvent simpleSaveEvent) {

        final BaseObject baseObject = simpleSaveEvent.getBaseObject();

        LOGGER.info("Object that is about to be saved: {}", baseObject);
    }
}
