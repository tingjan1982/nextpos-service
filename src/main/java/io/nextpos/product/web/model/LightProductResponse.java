package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductSet;
import io.nextpos.product.data.ProductType;
import io.nextpos.product.data.ProductVersion;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class LightProductResponse {

    private String id;

    private ProductType productType;

    private String name;

    private String description;

    private BigDecimal price;

    private String productLabelId;

    private boolean pinned;

    private boolean outOfStock;

    private List<ChildProduct> childProducts;

    public LightProductResponse(ProductVersion product) {

        ProductLabel productLabel = product.getProduct().getProductLabel();
        String productLabelId = productLabel != null ? productLabel.getId() : null;

        id = product.getProduct().getId();
        productType = ProductType.resolveProductType(product.getProduct());
        name = product.getProductName();
        description = product.getDescription();
        price = product.getPrice();
        this.productLabelId = productLabelId;
        pinned = product.getProduct().isPinned();
        outOfStock = product.getProduct().isOutOfStock();

        if (product.getProduct() instanceof ProductSet) {
            childProducts = ChildProduct.toChildProducts(((ProductSet) product.getProduct()));
        }
    }
}
