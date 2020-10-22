package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.service.ProductOptionService;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductOptionRelationRepositoryTest {

    @Autowired
    private ProductOptionRelationRepository productOptionRelationRepository;

    @Autowired
    private ProductOptionService productOptionService;

    @Autowired
    private ProductLabelService productLabelService;

    @Autowired
    private ClientService clientService;

    @Test
    void findAllByProductOption() {

        Client client = DummyObjects.dummyClient();
        clientService.saveClient(client);

        final ProductOption productOption = new ProductOption(client, DummyObjects.dummyProductOptionVersion());
        productOptionService.saveProductOption(productOption);

        final ProductLabel label = new ProductLabel("label", client);
        productLabelService.saveProductLabel(label);

        final ProductOptionRelation.ProductOptionOfLabel productOptionOfLabel = new ProductOptionRelation.ProductOptionOfLabel(productOption, label);
        productOptionRelationRepository.save(productOptionOfLabel);

        final Long relationCount = productOptionRelationRepository.countByProductOption(productOption);

        assertThat(relationCount).isEqualTo(1);
    }
}