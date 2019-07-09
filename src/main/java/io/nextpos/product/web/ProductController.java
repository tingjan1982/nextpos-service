package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.service.ProductService;
import io.nextpos.product.web.model.ProductRequest;
import io.nextpos.product.web.model.ProductResponse;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    private final ProductLabelService productLabelService;

    @Autowired
    public ProductController(final ProductService productService, final ProductLabelService productLabelService) {
        this.productService = productService;
        this.productLabelService = productLabelService;
    }

    @PostMapping
    public ProductResponse createProduct(@RequestAttribute("req-client") Client client, @Valid @RequestBody ProductRequest productRequest) {

        final Product product = fromRequest(productRequest, client);
        final Product createdProduct = productService.createProduct(product);

        return toResponse(createdProduct);
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable String id) {

        final Product product = productService.getProduct(id);

        return toResponse(product);
    }

    private Product fromRequest(ProductRequest productRequest, Client client) {

        final ProductVersion productVersion = new ProductVersion(productRequest.getName(),
                productRequest.getSku(),
                productRequest.getDescription(),
                productRequest.getPrice());
        final Product product = new Product(client, productVersion);

        if (productRequest.getProductLabelId() != null) {
            final ProductLabel productLabel = productLabelService.getProductLabel(productRequest.getProductLabelId()).orElseThrow(() -> {
                throw new ObjectNotFoundException(productRequest.getProductLabelId(), ProductLabel.class);
            });

            product.setProductLabel(productLabel);
        }

        return product;
    }

    private ProductResponse toResponse(Product product) {

        final ProductVersion latestVersion = product.getLatestVersion();
        final ProductLabel productLabel = product.getProductLabel();

        return new ProductResponse(product.getId(),
                latestVersion.getProductName(),
                latestVersion.getSku(),
                latestVersion.getDescription(),
                latestVersion.getPrice(),
                productLabel != null ? productLabel.getName() : null);
    }
}
