package io.nextpos.product.service;

import io.nextpos.product.data.ProductOption;

public interface ProductOptionService {

    ProductOption createProductOption(ProductOption productOption);

    ProductOption getProductOption(String id);

    void deployProductOption(String id);
}
