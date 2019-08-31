package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductOption;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductLabelServiceImplTest {

    @Autowired
    private ProductLabelService productLabelService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductOptionService productOptionService;

    @Autowired
    private ClientService clientService;

    private Client client;

    @BeforeEach
    void prepare () {
        final Client c = DummyObjects.dummyClient();
        client = clientService.createClient(c);
    }

    @Test
    void createAndGetProductLabelByName() {

        final ProductLabel productLabel = new ProductLabel("drink", client);
        productLabel.addChildProductLabel("coffee");
        productLabel.addChildProductLabel("tea");

        final ProductLabel createdProductLabel = productLabelService.saveProductLabel(productLabel);

        assertThat(createdProductLabel.getId()).isNotNull();
        assertThat(createdProductLabel.getClient()).isEqualTo(client);
        assertThat(createdProductLabel.getChildLabels()).hasSize(2);

        assertThat(productLabelService.getProductLabel(createdProductLabel.getId()).isPresent()).isTrue();

        final ProductLabel retrievedLabel = productLabelService.getProductLabelByName("drink", client)
                .orElseThrow();

        assertThat(retrievedLabel).isNotNull();
    }

    @Test
    void applyProductOptionsToProducts() {

        final ProductOption option1 = new ProductOption(client, DummyObjects.dummyProductOptionVersion());
        productOptionService.createProductOption(option1);

        final ProductOption option2 = new ProductOption(client, DummyObjects.dummyProductOptionVersion());
        productOptionService.createProductOption(option2);

        final ProductLabel drink = new ProductLabel("drink", client);
        drink.replaceProductOptions(option1, option2);
        productLabelService.saveProductLabel(drink);

        assertThat(drink.getProductOptionOfLabels()).hasSize(2);

        final Product product = new Product(client, DummyObjects.dummyProductVersion());
        product.setProductLabel(drink);

        productService.saveProduct(product);

        assertThat(product.getProductOptionOfProducts()).isEmpty();

        final List<Product> appliedProducts = productLabelService.applyProductOptionsToProducts(drink);

        assertThat(appliedProducts).hasSize(1);
        assertThat(product.getProductOptionOfProducts()).hasSize(2);
    }

    @Test
    void getProductLabels() {

        final List<ProductLabel> emptyList = productLabelService.getProductLabels(client);

        assertThat(emptyList).isNotNull();
        assertThat(emptyList).hasSize(0);

        productLabelService.saveProductLabel(new ProductLabel("label1", client));
        productLabelService.saveProductLabel(new ProductLabel("label2", client));
        productLabelService.saveProductLabel(new ProductLabel("label3", client));

        final List<ProductLabel> productLabels = productLabelService.getProductLabels(client);

        assertThat(productLabels).hasSize(3);
    }
}