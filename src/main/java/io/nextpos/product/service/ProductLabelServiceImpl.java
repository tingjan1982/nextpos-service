package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.*;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductLabelServiceImpl implements ProductLabelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductLabelServiceImpl.class);
    private final ProductLabelRepository productLabelRepository;

    private final ProductRepository productRepository;

    @Autowired
    public ProductLabelServiceImpl(final ProductLabelRepository productLabelRepository, final ProductRepository productRepository) {
        this.productLabelRepository = productLabelRepository;
        this.productRepository = productRepository;
    }

    @Override
    public ProductLabel saveProductLabel(final ProductLabel productLabel) {
        return productLabelRepository.save(productLabel);
    }

    @Override
    public Optional<ProductLabel> getProductLabel(final String id) {
        return productLabelRepository.findById(id);
    }

    @Override
    public ProductLabel getProductLabelOrThrows(final String id) {
        return this.getProductLabel(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, ProductLabel.class);
        });
    }

    @Override
    public Optional<ProductLabel> getProductLabelByName(final String name, final Client client) {
        return productLabelRepository.findByNameAndClient(name, client);
    }

    @Override
    public List<ProductLabel> getProductLabels(final Client client) {
        return productLabelRepository.findAllByClientAndParentLabelIsNull(client, Sort.by(Sort.Direction.ASC, "name"));
    }


    /**
     * @param productLabel
     * @return list of product that has got its product options replaced with ones in ProductLabel.
     */
    @Override
    public List<Product> applyProductLabelChangesToProducts(final ProductLabel productLabel) {

        final List<Product> products = productRepository.findAllByClientAndProductLabel(productLabel.getClient(), productLabel);

        if (products.isEmpty()) {
            return List.of();
        }

        final List<Product> productsToUpdate = products.stream()
                .peek(p -> p.setWorkingArea(productLabel.getWorkingArea()))
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(productLabel.getProductOptionOfLabels())) {
            final ProductOption[] productOptions = productLabel.getProductOptionOfLabels().stream()
                    .map(ProductOptionRelation.ProductOptionOfLabel::getProductOption).toArray(ProductOption[]::new);

            LOGGER.info("Applying {} product options to {} products belong to product label: {}", productOptions.length, productsToUpdate.size(), productLabel.getName());

            productsToUpdate.forEach(p -> p.replaceProductOptions(productOptions));
        }

        LOGGER.info("Saving {} product changes after applying product label changes.", productsToUpdate.size());

        return productsToUpdate.stream().map(productRepository::save).collect(Collectors.toList());
    }
}
