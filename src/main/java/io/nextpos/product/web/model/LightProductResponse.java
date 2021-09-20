package io.nextpos.product.web.model;

import io.nextpos.product.data.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.util.CollectionUtils;

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

    private boolean hasOptions;

    private boolean comboProduct;

    public LightProductResponse(ProductVersion product) {
        this(product, false);
    }

    public LightProductResponse(ProductVersion product, boolean hasOptionCheck) {

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

        comboProduct = product.getProduct() instanceof ProductCombo;

        if (hasOptionCheck) {
            hasOptions = !CollectionUtils.isEmpty(product.getProduct().getProductOptionOfProducts());
        }
    }
}
