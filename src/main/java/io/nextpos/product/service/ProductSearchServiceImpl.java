package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.product.data.ProductVersionRepository;
import io.nextpos.product.data.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductVersionRepository productVersionRepository;

    private final ProductLabelService productLabelService;

    @Autowired
    public ProductSearchServiceImpl(final ProductVersionRepository productVersionRepository, final ProductLabelService productLabelService) {
        this.productVersionRepository = productVersionRepository;
        this.productLabelService = productLabelService;
    }


    @Override
    public Map<ProductLabel, List<ProductVersion>> getAllProductsGroupedByLabels(final Client client, final Version version) {

        final List<ProductLabel> productLabels = productLabelService.getProductLabels(client);
        final ProductLabel ungroupedLabel = new ProductLabel("ungrouped", client);
        final Sort sortByProductName = Sort.by(Sort.Order.asc("productName"));

        final List<ProductVersion> products = productVersionRepository.findAllProductsByClient(client, version, sortByProductName);

        final Map<ProductLabel, List<ProductVersion>> groupedProducts = products.stream()
                .sorted(Comparator.comparing(ProductVersion::getProductName))
                .collect(Collectors.groupingBy(pv -> {
                    ProductLabel label = pv.getProduct().getProductLabel();
                    return label != null ? label : ungroupedLabel;
                }));

        final ProductLabel pinnedLabel = new ProductLabel("pinned", client);
        final List<ProductVersion> pinnedProducts = productVersionRepository.findAllByProduct_ClientAndVersionAndProduct_Pinned(client, version, true, sortByProductName);
        groupedProducts.put(pinnedLabel, pinnedProducts);

        final List<ProductLabel> labelsWithoutProduct = productLabels.stream()
                .filter(label -> !groupedProducts.containsKey(label))
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(labelsWithoutProduct)) {
            labelsWithoutProduct.forEach(label -> groupedProducts.put(label, List.of()));
        }

        return groupedProducts;
    }

}
