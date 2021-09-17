package io.nextpos.product.service;

import io.nextpos.product.data.*;

import java.util.List;

public interface ProductService {

    Product saveProduct(Product product);

    ProductSet saveProductSet(ProductSet productSet);

    ProductCombo saveProductCombo(ProductCombo productCombo);

    ParentProduct saveParentProduct(ParentProduct parentProduct);

    Product getProduct(String id);

    ProductSet getProductSet(String id);

    ProductCombo getProductCombo(String id);

    ParentProduct getParentProduct(String id);

    void deployProduct(String id);

    void reorderProducts(List<String> productIds);

    void deleteProduct(Product product);

    VariationDefinition saveVariationDefinition(VariationDefinition variationDefinition);

    VariationDefinition getVariationDefinition(String id);
}
