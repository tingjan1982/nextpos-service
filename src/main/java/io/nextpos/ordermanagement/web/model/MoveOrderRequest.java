package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLog;
import io.nextpos.shared.aspect.OrderLogChangeObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class MoveOrderRequest implements OrderLogChangeObject {

    @NotBlank
    private String targetOrderId;

    @Override
    public void populateOrderLogEntries(Order orderBeforeChange, Order orderAfterChange, OrderLog orderLog) {
        orderLog.addChangeOrderLogEntry(createLogEntryKey("table"), orderBeforeChange.getTableNames(), orderAfterChange.getTableNames());
    }
}
