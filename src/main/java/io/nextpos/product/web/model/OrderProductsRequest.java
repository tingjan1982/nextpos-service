package io.nextpos.product.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class OrderProductsRequest {

    private List<String> productIds = new ArrayList<>();
}
