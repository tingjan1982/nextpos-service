package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductOptionRelation;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.web.model.*;
import io.nextpos.product.web.util.ObjectWithProductOptionVisitorWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static io.nextpos.shared.web.ClientResolver.REQ_ATTR_CLIENT;

@RestController
@RequestMapping("/labels")
public class ProductLabelController {

    private final ProductLabelService productLabelService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    private final ObjectWithProductOptionVisitorWrapper productOptionVisitorWrapper;

    @Autowired
    public ProductLabelController(final ProductLabelService productLabelService, final ClientObjectOwnershipService clientObjectOwnershipService, final ObjectWithProductOptionVisitorWrapper productOptionVisitorWrapper) {
        this.productLabelService = productLabelService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
        this.productOptionVisitorWrapper = productOptionVisitorWrapper;
    }

    @PostMapping
    public ProductLabelResponse createProductLabel(@RequestAttribute(REQ_ATTR_CLIENT) Client client, @Valid @RequestBody ProductLabelRequest productLabelRequest) {

        ProductLabel productLabel = fromProductLabelRequest(productLabelRequest, client);
        final ProductLabel createdProductLabel = productLabelService.saveProductLabel(productLabel);

        return toProductLabelResponse(createdProductLabel);
    }

    @GetMapping("/{id}")
    public ProductLabelResponse getProductLabel(@RequestAttribute(REQ_ATTR_CLIENT) Client client,
                                                @PathVariable String id) {

        final ProductLabel productLabel = clientObjectOwnershipService.checkOwnership(client, () -> productLabelService.getProductLabelOrThrows(id));
        return toProductLabelResponse(productLabel);
    }

    @GetMapping
    public ProductLabelsResponse getProductLabels(@RequestAttribute(REQ_ATTR_CLIENT) Client client) {

        final List<ProductLabel> productLabels = productLabelService.getProductLabels(client);
        return toProductLabelsResponse(productLabels);

    }

    @PostMapping("/{id}")
    public ProductLabelResponse updateProductLabel(@RequestAttribute(REQ_ATTR_CLIENT) Client client,
                                                   @PathVariable final String id,
                                                   @Valid @RequestBody UpdateProductLabelRequest updateProductLabelRequest) {

        final ProductLabel productLabel = clientObjectOwnershipService.checkOwnership(client, () -> productLabelService.getProductLabelOrThrows(id));
        productLabel.setName(updateProductLabelRequest.getLabel());
        productOptionVisitorWrapper.accept(productLabel, updateProductLabelRequest.getProductOptionIds());

        final ProductLabel savedProductLabel = productLabelService.saveProductLabel(productLabel);

        return toProductLabelResponse(savedProductLabel);
    }

    @PostMapping("/{id}/applyOptions")
    public AppliedProductsResponse applyProductOptionsToProducts(@RequestAttribute(REQ_ATTR_CLIENT) Client client,
                                                                 @PathVariable final String id) {

        final ProductLabel productLabel = clientObjectOwnershipService.checkOwnership(client, () -> productLabelService.getProductLabelOrThrows(id));
        final List<Product> appliedProducts = productLabelService.applyProductOptionsToProducts(productLabel);

        final List<SimpleObjectResponse> appliedProductResponses = appliedProducts.stream()
                .map(p -> new SimpleObjectResponse(p.getId(), p.getDesignVersion().getProductName()))
                .collect(Collectors.toList());

        return new AppliedProductsResponse(appliedProductResponses);
    }

    private ProductLabelsResponse toProductLabelsResponse(final List<ProductLabel> productLabels) {
        final List<ProductLabelResponse> labelNames = productLabels.stream()
                .map(label -> new ProductLabelResponse(label.getId(), label.getName())).collect(Collectors.toList());

        return new ProductLabelsResponse(labelNames);
    }

    private ProductLabel fromProductLabelRequest(final ProductLabelRequest productLabelRequest, final Client client) {

        final ProductLabel productLabel = new ProductLabel(productLabelRequest.getLabel(), client);

        productOptionVisitorWrapper.accept(productLabel, productLabelRequest.getProductOptionIds());

        if (!CollectionUtils.isEmpty(productLabelRequest.getSubLabels())) {
            productLabelRequest.getSubLabels().forEach(subLabel -> this.addSubLabelsRecursively(subLabel, productLabel));
        }

        return productLabel;

    }

    private void addSubLabelsRecursively(ProductLabelRequest productLabelRequest, ProductLabel parentProductLabel) {

        final ProductLabel childLabel = parentProductLabel.addChildProductLabel(productLabelRequest.getLabel());

        if (CollectionUtils.isEmpty(productLabelRequest.getSubLabels())) {
            return;
        }

        productLabelRequest.getSubLabels().forEach(subLabel -> addSubLabelsRecursively(subLabel, childLabel));
    }

    private ProductLabelResponse toProductLabelResponse(final ProductLabel productLabel) {

        final ProductLabelResponse productLabelResponse = new ProductLabelResponse(productLabel.getId(), productLabel.getName());

        final List<SimpleObjectResponse> productOptions = productLabel.getProductOptionOfLabels().stream()
                .filter(pol -> pol.getProductLabel() != null)
                .map(ProductOptionRelation::getProductOption)
                .map(po -> new SimpleObjectResponse(po.getId(), po.getDesignVersion().getOptionName())).collect(Collectors.toList());

        productLabelResponse.setProductOptions(productOptions);

        if (!CollectionUtils.isEmpty(productLabel.getChildLabels())) {
            productLabel.getChildLabels().forEach(childLabel -> addSubLabelsRecursively(childLabel, productLabelResponse));
        }

        return productLabelResponse;
    }

    private void addSubLabelsRecursively(ProductLabel childLabel, ProductLabelResponse parentLabelResponse) {

        final ProductLabelResponse subLabelResponse = parentLabelResponse.addSubLabel(childLabel);

        if (CollectionUtils.isEmpty(childLabel.getChildLabels())) {
            return;
        }

        childLabel.getChildLabels().forEach(subLabel -> addSubLabelsRecursively(subLabel, subLabelResponse));
    }
}
