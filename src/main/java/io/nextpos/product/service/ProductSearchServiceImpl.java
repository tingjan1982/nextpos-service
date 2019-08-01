package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.product.data.ProductVersionRepository;
import io.nextpos.shared.model.BusinessObjectState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductVersionRepository productVersionRepository;

    @Autowired
    public ProductSearchServiceImpl(final ProductVersionRepository productVersionRepository) {
        this.productVersionRepository = productVersionRepository;
    }


    @Override
    public Map<ProductLabel, List<ProductVersion>> getAllProductsGroupedByLabels(final Client client, final BusinessObjectState state) {

        final List<ProductVersion> products = productVersionRepository.findAllProductsByClient(client, state, Sort.by(Sort.Order.asc("productName")));
        return products.stream().collect(Collectors.groupingBy(pv -> pv.getProduct().getProductLabel()));
    }

}
