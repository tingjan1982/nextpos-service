package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    private String tableId;

    private Order.DemographicData demographicData;

    private List<OrderLineItemRequest> lineItems;
}
