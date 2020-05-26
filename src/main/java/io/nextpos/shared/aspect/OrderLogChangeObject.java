package io.nextpos.shared.aspect;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLog;

public interface OrderLogChangeObject {

    void populateOrderLogEntries(final Order orderBeforeChange, Order orderAfterChange, OrderLog orderLog);
}
