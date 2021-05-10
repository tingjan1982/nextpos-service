package io.nextpos.product.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class OrderProductLabelRequest {

    @Deprecated
    private int index;

    @Deprecated
    private String previousProductLabelId;

    @Deprecated
    private String nextProductLabelId;

    private List<String> productLabelIds = new ArrayList<>();
}
