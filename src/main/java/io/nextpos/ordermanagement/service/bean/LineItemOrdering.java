package io.nextpos.ordermanagement.service.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineItemOrdering {

    private String orderId;

    private String lineItemId;
}
