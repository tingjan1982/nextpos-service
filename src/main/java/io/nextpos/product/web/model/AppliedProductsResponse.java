package io.nextpos.product.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AppliedProductsResponse {

    private List<SimpleObjectResponse> appliedProducts;
}
