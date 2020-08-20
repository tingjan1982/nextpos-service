package io.nextpos.product.service;

import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductSet;

public interface ProductService {

    Product saveProduct(Product product);

    ProductSet saveProductSet(ProductSet productSet);

    Product getProduct(String id);

    ProductSet getProductSet(String id);

    void deployProduct(String id);

    void deleteProduct(Product product);
}
