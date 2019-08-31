package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.*;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductLabelServiceImpl implements ProductLabelService {

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
        return productLabelRepository.findAllByClientAndParentLabelIsNull(client);
    }


    /**
     * todo: add more logs here
     *
     * @param productLabel
     * @return list of product that has got its product options replaced with ones in ProductLabel.
     */
    @Override
    public List<Product> applyProductOptionsToProducts(final ProductLabel productLabel) {

        if (!CollectionUtils.isEmpty(productLabel.getProductOptionOfLabels())) {
            final List<Product> products = productRepository.findAllByClientAndProductLabel(productLabel.getClient(), productLabel);

            if (!products.isEmpty()) {
                final ProductOption[] productOptions = productLabel.getProductOptionOfLabels().stream()
                        .map(ProductOptionRelation.ProductOptionOfLabel::getProductOption).toArray(ProductOption[]::new);

                return products.stream()
                        .map(p -> {
                            p.replaceProductOptions(productOptions);
                            return productRepository.save(p);

                        }).collect(Collectors.toList());
            }
        }

        return List.of();
    }
}
