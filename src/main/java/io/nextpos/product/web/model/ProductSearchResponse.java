package io.nextpos.product.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ProductSearchResponse {

    private Map<String, List<LightProductResponse>> results;

}
