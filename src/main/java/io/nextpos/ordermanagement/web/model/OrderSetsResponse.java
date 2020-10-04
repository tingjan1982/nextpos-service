package io.nextpos.ordermanagement.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OrderSetsResponse {

    private List<OrderSetResponse> results;
}
