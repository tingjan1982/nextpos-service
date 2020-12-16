package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductSet;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class ChildProduct {

    private String id;

    private String name;

    private String internalName;

    public static List<ChildProduct> toChildProducts(ProductSet productSet) {
        return productSet.getChildProducts().stream()
                .map(cp -> new ChildProduct(cp.getId(),
                        cp.getDesignVersion().getProductName(),
                        cp.getDesignVersion().getInternalProductName())).collect(Collectors.toList());
    }
}
