package io.nextpos.product.web;

import io.nextpos.product.web.model.ProductRequest;
import io.nextpos.product.web.model.ProductResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    @PostMapping("")
    public ProductResponse createProduct(@RequestBody ProductRequest productRequest) {

        return new ProductResponse("uuid", productRequest.getName(), productRequest.getDescription(), productRequest.getPrice());
    }
}
