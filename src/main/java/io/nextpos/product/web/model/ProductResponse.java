package io.nextpos.product.web.model;

import io.nextpos.inventorymanagement.web.model.InventoryResponse;
import io.nextpos.product.data.*;
import io.nextpos.workingarea.data.WorkingArea;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
public class ProductResponse {

    private final String id;

    private final ProductType productType;

    private final String versionId;

    private final String name;

    private final String internalName;

    private final Version version;

    private final String description;

    private final BigDecimal price;

    private final BigDecimal costPrice;

    private final String productLabelId;

    private final String productLabel;

    // todo: consolidate working area id and product option ids in product and product label.
    private final String workingAreaId;

    private final List<String> productOptionIds;

    private final List<ProductOptionResponse> productOptions;

    private final boolean pinned;

    private final boolean outOfStock;

    private List<ChildProduct> childProducts;

    private List<ProductComboLabelResponse> productComboLabels;

    private InventoryResponse inventory;

    public ProductResponse(Product product, ProductVersion productVersion) {

        id = product.getId();
        productType = ProductType.resolveProductType(product);
        versionId = productVersion.getId();
        name = productVersion.getProductName();
        internalName = productVersion.getInternalProductName();
        version = productVersion.getVersion();
        description = productVersion.getDescription();
        price = productVersion.getPrice();
        costPrice = productVersion.getCostPrice();

        final ProductLabel productLabel = product.getProductLabel();
        final WorkingArea workingArea = product.getWorkingArea();

        productLabelId = productLabel != null ? productLabel.getId() : null;
        this.productLabel = productLabel != null ? productLabel.getName() : null;
        workingAreaId = workingArea != null ? workingArea.getId() : null;

        productOptionIds = product.getProductOptionOfProducts().stream()
                .map(po -> po.getProductOption().getId()).collect(Collectors.toList());

        productOptions = product.getProductOptionOfProducts().stream()
                .map(po -> toProductOptionResponse(version, po))
                .collect(Collectors.toList());

        pinned = product.isPinned();
        outOfStock = product.isOutOfStock();
    }

    private ProductOptionResponse toProductOptionResponse(final Version version, final ProductOptionRelation.ProductOptionOfProduct po) {

        final ProductOptionVersion productOptionVersion = po.getProductOption().getObjectByVersionThrows(version);
        return ProductOptionResponse.fromProductOptionVersion(productOptionVersion);
    }
}
