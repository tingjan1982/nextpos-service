package io.nextpos.ordermanagement.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    private String tableId;

    private List<OrderLineItemRequest> lineItems;
}
