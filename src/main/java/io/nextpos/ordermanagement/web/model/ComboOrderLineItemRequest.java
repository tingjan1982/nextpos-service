package io.nextpos.ordermanagement.web.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class ComboOrderLineItemRequest extends OrderLineItemRequest {

    private List<OrderLineItemRequest> childLineItems = new ArrayList<>();
}
