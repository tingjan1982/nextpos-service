package io.nextpos.product.service;

import io.nextpos.product.data.Product;

public interface ProductService {

    Product createProduct(Product product);

    Product getProduct(String id);

    void deleteProduct(Product product);
}
