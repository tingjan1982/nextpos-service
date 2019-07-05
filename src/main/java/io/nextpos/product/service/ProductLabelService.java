package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.ProductLabel;

import java.util.Optional;

public interface ProductLabelService {

    ProductLabel createProductLabel(ProductLabel productLabel);

    ProductLabel getProductLabel(String id);

    Optional<ProductLabel> getProductLabelByName(String name, final Client client);
}
