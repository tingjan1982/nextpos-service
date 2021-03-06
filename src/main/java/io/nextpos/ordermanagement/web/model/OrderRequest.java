package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLog;
import io.nextpos.shared.aspect.OrderLogChangeObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest implements OrderLogChangeObject {

    @NotNull
    private Order.OrderType orderType = Order.OrderType.IN_STORE;

    private List<String> tableIds = new ArrayList<>();

    private Order.DemographicData demographicData;

    private String membershipId;

    private List<OrderLineItemRequest> lineItems;

    @Override
    public void populateOrderLogEntries(final Order orderBeforeChange, final Order orderAfterChange, final OrderLog orderLog) {

        if (orderBeforeChange.getOrderType() != orderType) {
            orderLog.addChangeOrderLogEntry("orderType", orderBeforeChange.getOrderType().name(), orderType.name());
        }

        if (orderType == Order.OrderType.IN_STORE && !StringUtils.equals(orderBeforeChange.getTableNames(), orderAfterChange.getTableNames())) {
            orderLog.addChangeOrderLogEntry("table", orderBeforeChange.getTableNames(), orderAfterChange.getTableNames());
        }

        if (orderAfterChange.getMembership() != null) {
            orderLog.addOrderLogEntry("membership", orderAfterChange.getMembership().getName());
        }
    }
}
