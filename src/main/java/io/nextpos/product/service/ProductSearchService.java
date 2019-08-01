package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.shared.model.BusinessObjectState;

import java.util.List;
import java.util.Map;

public interface ProductSearchService {

    Map<ProductLabel, List<ProductVersion>> getAllProductsGroupedByLabels(Client client, BusinessObjectState state);
}
