package io.nextpos.product.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderProductLabelRequest {

    private int index;
    
    private String previousProductLabelId;

    private String nextProductLabelId;
}
