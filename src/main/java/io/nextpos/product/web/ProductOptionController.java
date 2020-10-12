package io.nextpos.product.web;

import io.micrometer.core.instrument.util.StringUtils;
import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.product.data.*;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.service.ProductOptionService;
import io.nextpos.product.web.model.ProductOptionRequest;
import io.nextpos.product.web.model.ProductOptionResponse;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.shared.web.model.SimpleObjectResponse;
import io.nextpos.shared.web.model.SimpleObjectsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/productoptions")
public class ProductOptionController {

    private final ProductOptionService productOptionService;

    private final ProductLabelService productLabelService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    @Autowired
    public ProductOptionController(final ProductOptionService productOptionService, final ProductLabelService productLabelService, final ClientObjectOwnershipService clientObjectOwnershipService) {
        this.productOptionService = productOptionService;
        this.productLabelService = productLabelService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
    }


    @PostMapping
    public ProductOptionResponse createProductOption(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @Valid @RequestBody ProductOptionRequest productOptionRequest) {

        final ProductOption productOption = fromProductOptionRequest(client, productOptionRequest);
        final ProductOption createdProductOption = productOptionService.saveProductOption(productOption);

        return toProductOptionResponse(createdProductOption, Version.DESIGN);
    }

    @GetMapping("/{id}")
    public ProductOptionResponse getProductOption(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                  @PathVariable String id,
                                                  @RequestParam(value = "version", required = false, defaultValue = "DESIGN") Version version) {

        final ProductOption productOption = clientObjectOwnershipService.checkOwnership(client, () -> productOptionService.getProductOption(id));
        final ProductOptionResponse response = toProductOptionResponse(productOption, version);

        final List<? extends ProductOptionRelation> relations = productOptionService.getProductOptionRelationsByProductOption(productOption);

        final List<SimpleObjectResponse> usedByLabels = relations.stream()
                .filter(r -> r instanceof ProductOptionRelation.ProductOptionOfLabel)
                .map(r -> ((ProductOptionRelation.ProductOptionOfLabel) r))
                .map(r -> new SimpleObjectResponse(r.getProductLabel().getId(), r.getProductLabel().getName()))
                .collect(Collectors.toList());
        response.setUsedByProductLabels(usedByLabels);

        final List<SimpleObjectResponse> usedByProducts = relations.stream()
                .filter(r -> r instanceof ProductOptionRelation.ProductOptionOfProduct)
                .map(r -> ((ProductOptionRelation.ProductOptionOfProduct) r))
                .map(r -> new SimpleObjectResponse(r.getProduct().getId(), r.getProduct().getDesignVersion().getProductName()))
                .collect(Collectors.toList());
        response.setUsedByProducts(usedByProducts);

        return response;
    }

    @GetMapping
    public SimpleObjectsResponse getProductOptions(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                   @RequestParam(name = "productLabelId", required = false) String productLabelId) {

        List<ProductOptionVersion> productOptions;

        if (StringUtils.isNotBlank(productLabelId)) {
            final ProductLabel productLabel = clientObjectOwnershipService.checkOwnership(client, () -> productLabelService.getProductLabelOrThrows(productLabelId));
            productOptions = productOptionService.getProductOptionsByProductLabel(client, Version.DESIGN, productLabel);

        } else {
            productOptions = productOptionService.getProductOptions(client, Version.DESIGN);
        }

        final List<SimpleObjectResponse> results = productOptions.stream()
                .map(po -> new SimpleObjectResponse(po.getProductOption().getId(), po.getOptionName()))
                .collect(Collectors.toList());

        return new SimpleObjectsResponse(results);
    }

    @PostMapping("/{id}")
    public ProductOptionResponse updateProductOption(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                     @PathVariable final String id,
                                                     @Valid @RequestBody ProductOptionRequest productOptionRequest) {

        final ProductOption productOption = clientObjectOwnershipService.checkOwnership(client, () -> productOptionService.getProductOption(id));
        updateProductOptionFromRequest(productOption, productOptionRequest);
        final ProductOption createdProductOption = productOptionService.saveProductOption(productOption);

        return toProductOptionResponse(createdProductOption, Version.DESIGN);
    }

    private void updateProductOptionFromRequest(final ProductOption productOption, final ProductOptionRequest productOptionRequest) {

        final ProductOptionVersion.OptionType optionType = ProductOptionVersion.OptionType.valueOf(productOptionRequest.getOptionType());

        final ProductOptionVersion optionVersion = productOption.getDesignVersion();
        optionVersion.setOptionName(productOptionRequest.getOptionName());
        optionVersion.setOptionType(optionType);
        optionVersion.setRequired(productOptionRequest.isRequired());

        optionVersion.clearOptionValues();

        if (!CollectionUtils.isEmpty(productOptionRequest.getOptionValues())) {
            productOptionRequest.getOptionValues().forEach(value -> optionVersion.addOptionValue(value.getValue(), value.getPrice()));
        }
    }

    @PostMapping("/{id}/deploy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deployProductOption(@PathVariable String id) {

        productOptionService.deployProductOption(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProductOption(@PathVariable final String id) {

        final ProductOption productOption = productOptionService.getProductOption(id);
        productOptionService.deleteProductOption(productOption);
    }

    private ProductOption fromProductOptionRequest(Client client, ProductOptionRequest productOptionRequest) {

        final ProductOptionVersion.OptionType optionType = ProductOptionVersion.OptionType.valueOf(productOptionRequest.getOptionType());
        final ProductOptionVersion stagingProductOption = new ProductOptionVersion(productOptionRequest.getOptionName(), optionType, productOptionRequest.isRequired());

        if (!CollectionUtils.isEmpty(productOptionRequest.getOptionValues())) {
            productOptionRequest.getOptionValues().forEach(value -> stagingProductOption.addOptionValue(value.getValue(), value.getPrice()));
        }

        return new ProductOption(client, stagingProductOption);
    }

    private ProductOptionResponse toProductOptionResponse(final ProductOption productOption, final Version version) {

        ProductOptionVersion productOptionVersion = productOption.getObjectByVersionThrows(version);
        return ProductOptionResponse.fromProductOptionVersion(productOptionVersion);
    }
}
