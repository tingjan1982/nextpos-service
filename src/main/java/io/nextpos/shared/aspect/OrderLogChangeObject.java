package io.nextpos.shared.aspect;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLog;

public interface OrderLogChangeObject {

    String LOG_ENTRY_PREFIX = "logEntry.";

    default String createLogEntryKey(String keyName) {
        return keyName;
    }

    void populateOrderLogEntries(final Order orderBeforeChange, Order orderAfterChange, OrderLog orderLog);
}
