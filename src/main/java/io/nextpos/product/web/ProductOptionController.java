package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.ProductOption;
import io.nextpos.product.data.ProductOptionVersion;
import io.nextpos.product.data.Version;
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

    @Autowired
    public ProductOptionController(final ProductOptionService productOptionService) {
        this.productOptionService = productOptionService;
    }


    @PostMapping
    public ProductOptionResponse createProductOption(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @Valid @RequestBody ProductOptionRequest productOptionRequest) {

        final ProductOption productOption = fromProductOptionRequest(client, productOptionRequest);
        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        return toProductOptionResponse(createdProductOption, Version.DESIGN);
    }

    @GetMapping("/{id}")
    public ProductOptionResponse getProductOption(@PathVariable String id, @RequestParam(value = "version", required = false, defaultValue = "DESIGN") Version version) {

        final ProductOption productOption = productOptionService.getProductOption(id);

        return toProductOptionResponse(productOption, version);
    }

    @GetMapping
    public SimpleObjectsResponse getProductOptions(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        List<ProductOption> productOptions = productOptionService.getProductOptions(client);
        final List<SimpleObjectResponse> results = productOptions.stream()
                .map(po -> new SimpleObjectResponse(po.getId(), po.getDesignVersion().getOptionName()))
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
