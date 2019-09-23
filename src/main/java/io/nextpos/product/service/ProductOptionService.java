package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.ProductOption;

import java.util.List;

public interface ProductOptionService {

    ProductOption createProductOption(ProductOption productOption);

    ProductOption getProductOption(String id);

    List<ProductOption> getProductOptions(Client client);

    ProductOption deployProductOption(String id);
}
