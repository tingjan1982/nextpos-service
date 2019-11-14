package io.nextpos.product.web;

import io.micrometer.core.instrument.util.StringUtils;
import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductOption;
import io.nextpos.product.data.ProductOptionVersion;
import io.nextpos.product.data.Version;
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
        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        return toProductOptionResponse(createdProductOption, Version.DESIGN);
    }

    @GetMapping("/{id}")
    public ProductOptionResponse getProductOption(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                  @PathVariable String id,
                                                  @RequestParam(value = "version", required = false, defaultValue = "DESIGN") Version version) {

        final ProductOption productOption = clientObjectOwnershipService.checkOwnership(client, () -> productOptionService.getProductOption(id));

        return toProductOptionResponse(productOption, version);
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

    @PostMapping("/{id}/deploy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deployProductOption(@PathVariable String id) {

        productOptionService.deployProductOption(id);
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
