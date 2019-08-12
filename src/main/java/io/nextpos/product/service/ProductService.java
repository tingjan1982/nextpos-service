package io.nextpos.product.service;

import io.nextpos.product.data.Product;

public interface ProductService {

    Product saveProduct(Product product);

    Product getProduct(String id);

    void deployProduct(String id);

    void deleteProduct(Product product);
}
