package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductOption;
import io.nextpos.product.data.ProductOptionRelation;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.web.model.*;
import io.nextpos.product.web.util.ObjectWithProductOptionVisitorWrapper;
import io.nextpos.shared.web.model.SimpleObjectResponse;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    private final WorkingAreaService workingAreaService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    private final ObjectWithProductOptionVisitorWrapper productOptionVisitorWrapper;

    @Autowired
    public ProductLabelController(final ProductLabelService productLabelService, final WorkingAreaService workingAreaService, final ClientObjectOwnershipService clientObjectOwnershipService, final ObjectWithProductOptionVisitorWrapper productOptionVisitorWrapper) {
        this.productLabelService = productLabelService;
        this.workingAreaService = workingAreaService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
        this.productOptionVisitorWrapper = productOptionVisitorWrapper;
    }

    @PostMapping
    public ProductLabelResponse createProductLabel(@RequestAttribute(REQ_ATTR_CLIENT) Client client, @Valid @RequestBody ProductLabelRequest productLabelRequest) {

        ProductLabel productLabel = fromProductLabelRequest(productLabelRequest, client);
        final ProductLabel createdProductLabel = productLabelService.saveProductLabel(productLabel);

        return toProductLabelResponse(createdProductLabel);
    }

    private ProductLabel fromProductLabelRequest(final ProductLabelRequest productLabelRequest, final Client client) {

        final ProductLabel productLabel = new ProductLabel(productLabelRequest.getLabel(), client);

        resolveWorkingArea(client, productLabel, productLabelRequest.getWorkingAreaId());

        productOptionVisitorWrapper.accept(productLabel, productLabelRequest.getProductOptionIds());

        if (!CollectionUtils.isEmpty(productLabelRequest.getSubLabels())) {
            productLabelRequest.getSubLabels().forEach(subLabel -> this.addSubLabelsRecursively(subLabel, productLabel));
        }

        return productLabel;
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
        updateProductLabelFromRequest(client, productLabel, updateProductLabelRequest);

        final ProductLabel savedProductLabel = productLabelService.saveProductLabel(productLabel);

        final ProductLabelResponse productLabelResponse = toProductLabelResponse(savedProductLabel);

        if (updateProductLabelRequest.isAppliesToProducts()) {
            final List<Product> products = productLabelService.applyProductLabelChangesToProducts(savedProductLabel);
            final List<SimpleObjectResponse> simpleProducts = products.stream()
                    .map(p -> new SimpleObjectResponse(p.getId(), p.getDesignVersion().getProductName())).collect(Collectors.toList());

            productLabelResponse.setAppliedProducts(simpleProducts);
        }

        return productLabelResponse;
    }

    private void updateProductLabelFromRequest(@RequestAttribute(REQ_ATTR_CLIENT) Client client, ProductLabel productLabel, UpdateProductLabelRequest updateProductLabelRequest) {

        productLabel.setName(updateProductLabelRequest.getLabel());

        resolveWorkingArea(client, productLabel, updateProductLabelRequest.getWorkingAreaId());

        productOptionVisitorWrapper.accept(productLabel, updateProductLabelRequest.getProductOptionIds());
    }

    private void resolveWorkingArea(final Client client, final ProductLabel productLabel, final String workingAreaId) {

        if (workingAreaId != null) {
            final WorkingArea workingArea = clientObjectOwnershipService.checkOwnership(client, () -> workingAreaService.getWorkingArea(workingAreaId));
            productLabel.setWorkingArea(workingArea);
        } else {
            productLabel.setWorkingArea(null);
        }
    }

    @PostMapping("/{id}/order")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void updateProductLabel(@PathVariable final String id,
                                   @Valid @RequestBody OrderProductLabelRequest request) {

        final String previousProductLabelId = StringUtils.defaultIfBlank(request.getPreviousProductLabelId(), "");
        final String nextProductLabelId = StringUtils.defaultIfBlank(request.getNextProductLabelId(), "");
        
        productLabelService.updateProductLabelOrder(id, request.getIndex(), previousProductLabelId, nextProductLabelId);
    }

    @PostMapping("/{id}/applyOptions")
    public AppliedProductsResponse applyProductLabelChangesToProducts(@RequestAttribute(REQ_ATTR_CLIENT) Client client,
                                                                      @PathVariable final String id) {

        final ProductLabel productLabel = clientObjectOwnershipService.checkOwnership(client, () -> productLabelService.getProductLabelOrThrows(id));
        final List<Product> appliedProducts = productLabelService.applyProductLabelChangesToProducts(productLabel);

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

    private void addSubLabelsRecursively(ProductLabelRequest productLabelRequest, ProductLabel parentProductLabel) {

        final ProductLabel childLabel = parentProductLabel.addChildProductLabel(productLabelRequest.getLabel());

        if (CollectionUtils.isEmpty(productLabelRequest.getSubLabels())) {
            return;
        }

        productLabelRequest.getSubLabels().forEach(subLabel -> addSubLabelsRecursively(subLabel, childLabel));
    }

    private ProductLabelResponse toProductLabelResponse(final ProductLabel productLabel) {

        final ProductLabelResponse productLabelResponse = new ProductLabelResponse(productLabel.getId(), productLabel.getName());

        final WorkingArea workingArea = productLabel.getWorkingArea();

        if (workingArea != null) {
            final SimpleObjectResponse workingAreaResponse = new SimpleObjectResponse(workingArea.getId(), workingArea.getName());
            productLabelResponse.setWorkingArea(workingAreaResponse);
            productLabelResponse.setWorkingAreaId(workingArea.getId());
        }

        final List<SimpleObjectResponse> productOptions = productLabel.getProductOptionOfLabels().stream()
                .map(ProductOptionRelation::getProductOption)
                .map(po -> new SimpleObjectResponse(po.getId(), po.getDesignVersion().getOptionName())).collect(Collectors.toList());

        productLabelResponse.setProductOptions(productOptions);

        final List<String> productOptionIds = productLabel.getProductOptionOfLabels().stream()
                .map(ProductOptionRelation::getProductOption)
                .map(ProductOption::getId).collect(Collectors.toList());

        productLabelResponse.setProductOptionIds(productOptionIds);

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
