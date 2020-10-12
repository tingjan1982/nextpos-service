package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.*;

import java.util.List;

public interface ProductOptionService {

    ProductOption saveProductOption(ProductOption productOption);

    ProductOption getProductOption(String id);

    List<? extends ProductOptionRelation> getProductOptionRelationsByProductOption(ProductOption productOption);

    List<ProductOptionVersion> getProductOptions(Client client, final Version version);

    List<ProductOptionVersion> getProductOptionsByProductLabel(Client client, Version version, ProductLabel productLabel);

    void deleteProductOption(ProductOption productOption);

    ProductOption deployProductOption(String id);
}
