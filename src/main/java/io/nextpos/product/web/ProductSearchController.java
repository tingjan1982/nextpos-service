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
                                                            @RequestParam(value = "state", defaultValue = "DESIGN") Version version) {

        final Map<ProductLabel, List<ProductVersion>> groupedProducts = productSearchService.getAllProductsGroupedByLabels(client, version);

        return toProductSearchResponse(groupedProducts);
    }

    private ProductSearchResponse toProductSearchResponse(final Map<ProductLabel, List<ProductVersion>> groupedProducts) {

        final TreeMap<String, List<LightProductResponse>> results = new TreeMap<>();

        groupedProducts.forEach((key, value) -> {
            final List<LightProductResponse> products = value.stream()
                    .map(product -> {
                        ProductLabel productLabel = product.getProduct().getProductLabel();
                        String productLabelId = productLabel != null ? productLabel.getId() : null;

                        return new LightProductResponse(product.getProduct().getId(),
                                product.getProductName(),
                                product.getPrice(),
                                productLabelId,
                                product.getProduct().isPinned());
                    })
                    .collect(Collectors.toList());
            results.put(key.getName(), products);
        });

        return new ProductSearchResponse(results);
    }
}
