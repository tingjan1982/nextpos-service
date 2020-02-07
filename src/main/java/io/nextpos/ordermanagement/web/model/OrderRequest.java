package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull
    private Order.OrderType orderType = Order.OrderType.IN_STORE;

    private String tableId;

    private String tableNote;

    private Order.DemographicData demographicData;

    private List<OrderLineItemRequest> lineItems;
}
