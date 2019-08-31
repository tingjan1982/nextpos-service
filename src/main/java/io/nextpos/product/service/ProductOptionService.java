package io.nextpos.product.service;

import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductOption;
import io.nextpos.product.data.ProductOptionRelation;

import java.util.List;

public interface ProductOptionService {

    ProductOption createProductOption(ProductOption productOption);

    ProductOption getProductOption(String id);

    ProductOption deployProductOption(String id);

    List<ProductOptionRelation> addProductOptionToProduct(ProductOption productOption, List<Product> products);

    List<ProductOptionRelation> addProductOptionToProductLabel(ProductOption productOption, List<ProductLabel> productLabels);
}
