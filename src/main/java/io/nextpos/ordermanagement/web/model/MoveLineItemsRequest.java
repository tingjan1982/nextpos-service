package io.nextpos.ordermanagement.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class MoveLineItemsRequest extends UpdateLineItemsRequest {

    private String tableId;
}
