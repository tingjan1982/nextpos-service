package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductOption;
import io.nextpos.product.data.ProductOptionVersion;
import io.nextpos.product.data.Version;

import java.util.List;

public interface ProductOptionService {

    ProductOption saveProductOption(ProductOption productOption);

    ProductOption getProductOption(String id);

    List<ProductOptionVersion> getProductOptions(Client client, final Version version);

    List<ProductOptionVersion> getProductOptionsByProductLabel(Client client, Version version, ProductLabel productLabel);

    void deleteProductOption(ProductOption productOption);

    ProductOption deployProductOption(String id);
}
