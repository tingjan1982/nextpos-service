package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.web.model.ProductLabelRequest;
import io.nextpos.product.web.model.ProductLabelResponse;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import static io.nextpos.shared.web.ClientResolver.REQ_ATTR_CLIENT;

@RestController
@RequestMapping("/labels")
public class ProductLabelController {

    private final ProductLabelService productLabelService;

    @Autowired
    public ProductLabelController(final ProductLabelService productLabelService) {
        this.productLabelService = productLabelService;
    }

    @PostMapping
    public ProductLabelResponse createProductLabel(@RequestAttribute(REQ_ATTR_CLIENT) Client client, @RequestBody ProductLabelRequest productLabelRequest) {

        ProductLabel productLabel = fromProductLabelRequest(productLabelRequest, client);
        final ProductLabel createdProductLabel = productLabelService.createProductLabel(productLabel);

        return toProductLabelResponse(createdProductLabel);
    }

    @GetMapping("/{id}")
    public ProductLabelResponse getProductLabel(@PathVariable String id) {
        final ProductLabel productLabel = productLabelService.getProductLabel(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, ProductLabel.class);
        });

        return toProductLabelResponse(productLabel);
    }

    private ProductLabel fromProductLabelRequest(final ProductLabelRequest productLabelRequest, final Client client) {

        final ProductLabel productLabel = new ProductLabel(productLabelRequest.getLabel(), client);

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
