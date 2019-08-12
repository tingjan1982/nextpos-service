package io.nextpos.product.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResponse {

    private Map<String, List<ProductSearchResult>> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSearchResult {

        private String id;

        private String name;

        private BigDecimal price;
    }

}
