package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.ProductOption;
import io.nextpos.product.data.ProductOptionVersion;
import io.nextpos.product.service.ProductOptionService;
import io.nextpos.product.web.model.ProductOptionRequest;
import io.nextpos.product.web.model.ProductOptionResponse;
import io.nextpos.product.web.model.ProductOptionValueModel;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public ProductOptionResponse createProductOption(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @RequestBody ProductOptionRequest productOptionRequest) {

        final ProductOption productOption = fromProductOptionRequest(client, productOptionRequest);
        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        return toProductOptionResponse(createdProductOption);
    }

    private ProductOption fromProductOptionRequest(Client client, ProductOptionRequest productOptionRequest) {

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion(productOptionRequest.getOptionName(), productOptionRequest.getOptionType());
        productOptionRequest.getOptionValues().forEach(value -> stagingProductOption.addOptionValue(value.getOptionValue(), value.getOptionPrice()));

        return new ProductOption(client, stagingProductOption);
    }

    private ProductOptionResponse toProductOptionResponse(final ProductOption createdProductOption) {

        final ProductOptionVersion stagingProductOption = createdProductOption.getStagingProductOption();
        final List<ProductOptionValueModel> optionValues = stagingProductOption.getOptionValues().stream()
                .map(v -> new ProductOptionValueModel(v.getOptionValue(), v.getOptionPrice()))
                .collect(Collectors.toList());

        return new ProductOptionResponse(createdProductOption.getId(),
                stagingProductOption.getOptionName(),
                stagingProductOption.getOptionType(),
                optionValues);
    }


}
