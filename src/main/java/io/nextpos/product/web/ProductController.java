package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.product.service.ProductService;
import io.nextpos.product.web.model.ProductRequest;
import io.nextpos.product.web.model.ProductResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    private final String CLIENT = "req-client";

    @Autowired
    public ProductController(final ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("")
    public ProductResponse createProduct(@RequestAttribute("req-client") Client client, @RequestBody ProductRequest productRequest) {

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

        final Product product = new Product(client);
        final ProductVersion productVersion = new ProductVersion(productRequest.getName(),
                productRequest.getSku(),
                productRequest.getDescription(),
                productRequest.getPrice());

        product.addNewVersion(productVersion);

        return product;
    }

    private ProductResponse toResponse(Product product) {

        final ProductVersion latestVersion = product.getLatestVersion();
        return new ProductResponse(product.getId(),
                latestVersion.getProductName(),
                latestVersion.getSku(),
                latestVersion.getDescription(),
                latestVersion.getPrice());
    }
}
