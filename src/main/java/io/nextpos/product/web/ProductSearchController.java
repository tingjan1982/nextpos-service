package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.product.data.Version;
import io.nextpos.product.service.ProductSearchService;
import io.nextpos.product.web.model.LightProductResponse;
import io.nextpos.product.web.model.ProductSearchResponse;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/searches")
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @Autowired
    public ProductSearchController(final ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @GetMapping("/products/grouped")
    public ProductSearchResponse getProductsGroupedByLabels(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                            @RequestParam(value = "state", defaultValue = "DESIGN") Version version) {

        final Map<ProductLabel, List<ProductVersion>> groupedProducts = productSearchService.getAllProductsGroupedByLabels(client, version);

        return toProductSearchResponse(groupedProducts);
    }

    @GetMapping("/products")
    public ProductSearchResponse getProductsGroupedByLabels2(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                            @RequestParam(value = "state", defaultValue = "DESIGN") Version version) {

        final Map<ProductLabel, List<ProductVersion>> groupedProducts = productSearchService.getAllProductsGroupedByLabels(client, version);

        final LinkedHashMap<String, List<LightProductResponse>> results = new LinkedHashMap<>();

        groupedProducts.forEach((key, value) -> {
            final List<LightProductResponse> products = value.stream()
                    .map(p -> new LightProductResponse(p, true))
                    .collect(Collectors.toList());
            results.put(key.getName(), products);
        });

        return new ProductSearchResponse(results);
    }

    private ProductSearchResponse toProductSearchResponse(final Map<ProductLabel, List<ProductVersion>> groupedProducts) {

        final LinkedHashMap<String, List<LightProductResponse>> results = new LinkedHashMap<>();

        groupedProducts.forEach((key, value) -> {
            final List<LightProductResponse> products = value.stream()
                    .map(LightProductResponse::new)
                    .collect(Collectors.toList());
            results.put(key.getName(), products);
        });

        return new ProductSearchResponse(results);
    }
}
