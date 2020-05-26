package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLog;
import io.nextpos.shared.aspect.OrderLogChangeObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest implements OrderLogChangeObject {

    @NotNull
    private Order.OrderType orderType = Order.OrderType.IN_STORE;

    private String tableId;

    private String tableNote;

    private Order.DemographicData demographicData;

    private List<OrderLineItemRequest> lineItems;

    @Override
    public void populateOrderLogEntries(final Order orderBeforeChange, final Order orderAfterChange, final OrderLog orderLog) {

        orderLog.addChangeOrderLogEntry("orderType", orderBeforeChange.getOrderType().name(), orderType.name());

        if (orderType == Order.OrderType.IN_STORE) {
            orderLog.addChangeOrderLogEntry("table", orderBeforeChange.getTableInfo().getDisplayName(), orderAfterChange.getTableInfo().getDisplayName());
        }
    }
}
