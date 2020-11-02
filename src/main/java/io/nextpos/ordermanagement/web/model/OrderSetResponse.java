package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.OrderSet;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OrderSetResponse {

    private String id;

    private List<OrderSet.OrderSetDetails> linkedOrders;

    private String mainOrderId;

    private String tableLayoutId;

    private OrderSet.OrderSetStatus status;
}
