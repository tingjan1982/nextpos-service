package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.product.service.ProductSearchService;
import io.nextpos.product.web.model.ProductSearchResponse;
import io.nextpos.shared.model.BusinessObjectState;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
                                                            @RequestParam(value = "state", defaultValue = "DESIGN") BusinessObjectState state) {

        final Map<ProductLabel, List<ProductVersion>> groupedProducts = productSearchService.getAllProductsGroupedByLabels(client, state);

        return toProductSearchResponse(groupedProducts);
    }

    private ProductSearchResponse toProductSearchResponse(final Map<ProductLabel, List<ProductVersion>> groupedProducts) {

        final TreeMap<String, List<ProductSearchResponse.ProductSearchResult>> results = new TreeMap<>();

        groupedProducts.forEach((key, value) -> {
            final List<ProductSearchResponse.ProductSearchResult> products = value.stream()
                    .map(product -> new ProductSearchResponse.ProductSearchResult(product.getProductName(), product.getPrice()))
                    .collect(Collectors.toList());
            results.put(key.getName(), products);
        });

        return new ProductSearchResponse(results);
    }
}
