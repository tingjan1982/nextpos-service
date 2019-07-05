package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@Transactional
class ProductLabelServiceImplTest {

    @Autowired
    private ProductLabelService productLabelService;

    @Autowired
    private ClientService clientService;


    @Test
    void createAndGetProductLabelByName() {

        final Client client = DummyObjects.dummyClient();
        clientService.createClient(client);

        final ProductLabel productLabel = new ProductLabel("drink", client);
        productLabel.addChildProductLabel("coffee");
        productLabel.addChildProductLabel("tea");

        final ProductLabel createdProductLabel = productLabelService.createProductLabel(productLabel);

        assertThat(createdProductLabel.getId()).isNotNull();
        assertThat(createdProductLabel.getClient()).isEqualTo(client);
        assertThat(createdProductLabel.getChildLabels()).hasSize(2);

        assertThat(productLabelService.getProductLabel(createdProductLabel.getId())).isNotNull();

        final ProductLabel retrievedLabel = productLabelService.getProductLabelByName("drink", client)
                .orElseThrow();

        assertThat(retrievedLabel).isNotNull();

    }
}