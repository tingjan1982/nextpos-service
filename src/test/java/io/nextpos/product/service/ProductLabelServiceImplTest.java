package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductOption;
import io.nextpos.shared.DummyObjects;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Optional assertions:
 * https://github.com/joel-costigliola/assertj-examples/blob/master/assertions-examples/src/test/java/org/assertj/examples/OptionalAssertionsExamples.java
 * 
 */
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
    private WorkingAreaService workingAreaService;

    @Autowired
    private ClientService clientService;

    private Client client;

    @BeforeEach
    void prepare() {
        final Client c = DummyObjects.dummyClient();
        client = clientService.createClient(c);
    }

    @Test
    void createAndGetProductLabelByName() {

        final ProductLabel productLabel = new ProductLabel("drink", client);
        productLabel.addChildProductLabel("coffee");
        productLabel.addChildProductLabel("tea");

        final WorkingArea workingArea = new WorkingArea(client, "bar");
        workingAreaService.saveWorkingArea(workingArea);

        productLabel.setWorkingArea(workingArea);

        final ProductLabel createdProductLabel = productLabelService.saveProductLabel(productLabel);

        assertThat(createdProductLabel.getId()).isNotNull();
        assertThat(createdProductLabel.getClient()).isEqualTo(client);
        assertThat(createdProductLabel.getWorkingArea()).isEqualTo(workingArea);
        assertThat(createdProductLabel.getChildLabels()).hasSize(2);

        assertThat(productLabelService.getProductLabel(createdProductLabel.getId())).isNotEmpty();

        assertThat(productLabelService.getProductLabelByName("drink", client)).isNotEmpty();
    }

    @Test
    void applyProductOptionsToProducts() {

        final ProductOption option1 = new ProductOption(client, DummyObjects.dummyProductOptionVersion());
        productOptionService.saveProductOption(option1);

        final ProductOption option2 = new ProductOption(client, DummyObjects.dummyProductOptionVersion());
        productOptionService.saveProductOption(option2);

        final WorkingArea workingArea = DummyObjects.dummyWorkingArea(client);
        workingAreaService.saveWorkingArea(workingArea);

        final ProductLabel drink = new ProductLabel("drink", client);
        drink.replaceProductOptions(option1, option2);
        drink.setWorkingArea(workingArea);

        productLabelService.saveProductLabel(drink);

        assertThat(drink.getProductOptionOfLabels()).hasSize(2);

        final Product product = new Product(client, DummyObjects.dummyProductVersion());
        product.setProductLabel(drink);

        productService.saveProduct(product);

        assertThat(product.getProductOptionOfProducts()).isEmpty();
        assertThat(product.getWorkingArea()).isNull();

        final List<Product> appliedProducts = productLabelService.applyProductLabelChangesToProducts(drink);

        assertThat(appliedProducts).hasSize(1);
        assertThat(product.getProductOptionOfProducts()).hasSize(2);
        assertThat(product.getWorkingArea()).isEqualTo(workingArea);
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