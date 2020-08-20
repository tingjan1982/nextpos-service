package io.nextpos.product.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProductLabelsResponse {

    private List<ProductLabelResponse> labels;


}
