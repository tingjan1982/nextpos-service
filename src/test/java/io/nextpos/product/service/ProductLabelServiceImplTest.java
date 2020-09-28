package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductOption;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.ObjectAlreadyExistsException;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Optional assertions:
 * https://github.com/joel-costigliola/assertj-examples/blob/master/assertions-examples/src/test/java/org/assertj/examples/OptionalAssertionsExamples.java
 */
@SpringBootTest
@Transactional
class ProductLabelServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductLabelServiceImplTest.class);

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

    @Test
    void saveLabelWithSameNames() {

        productLabelService.saveProductLabel(new ProductLabel("label", client));
        assertThatThrownBy(() -> productLabelService.saveProductLabel(new ProductLabel("label", client))).isInstanceOf(ObjectAlreadyExistsException.class);
    }

    @Test
    void updateProductLabelOrder() {

        List<ProductLabel> labels = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            final String labelName = "label " + i;
            final ProductLabel label = addLabel(labels, labelName, labelName);
            productLabelService.saveProductLabel(label);
        }

        // https://stackoverflow.com/questions/50215341/java8-null-safe-comparison
        final Comparator<ProductLabel> comparatorToUse = Comparator.comparing(ProductLabel::getOrderKey, Comparator.nullsLast(Comparator.naturalOrder()));

        productLabelService.updateProductLabelOrder(labels.get(4).getId(), 1, labels.get(0).getId(), labels.get(1).getId()); // orderKey=1, 12, 2, null, null
        assertThat(productLabelService.getProductLabels(client)).isSortedAccordingTo(comparatorToUse);
        productLabelService.getProductLabels(client).forEach(l -> LOGGER.info("id: {}, name: {}, order: {}", l.getId(), l.getName(), l.getOrderKey()));

        productLabelService.updateProductLabelOrder(labels.get(3).getId(), 1, labels.get(4).getId(), labels.get(1).getId()); // orderKey=1, 12, 122, 2, null
        assertThat(productLabelService.getProductLabels(client)).isSortedAccordingTo(comparatorToUse);
        productLabelService.getProductLabels(client).forEach(l -> LOGGER.info("id: {}, name: {}, order: {}", l.getId(), l.getName(), l.getOrderKey()));

        productLabelService.updateProductLabelOrder(labels.get(2).getId(), 2, labels.get(4).getId(), labels.get(3).getId()); // orderKey=1, 12, 12122, 122, 2
        assertThat(productLabelService.getProductLabels(client)).isSortedAccordingTo(comparatorToUse);
        productLabelService.getProductLabels(client).forEach(l -> LOGGER.info("id: {}, name: {}, order: {}", l.getId(), l.getName(), l.getOrderKey()));
    }

    @Test
    @Disabled
    void testOrderingAlgorithm() {

        List<ProductLabel> labels = new ArrayList<>();

        addLabel(labels, "a", "bbcbc");
        addLabel(labels, "b", "bbcbc4");
        addLabel(labels, "c", "0b");
        addLabel(labels, "d", "bbc");
        addLabel(labels, "e", "00b");

        final Comparator<ProductLabel> comparatorToUse = Comparator.comparing(ProductLabel::getOrderKey, Comparator.nullsLast(Comparator.naturalOrder()));
        labels.sort(comparatorToUse);

        labels.forEach(System.out::println);
    }

    private ProductLabel addLabel(List<ProductLabel> labels, String labelName, String orderKey) {
        final ProductLabel label = new ProductLabel(labelName, client);
        label.setOrderKey(orderKey);

        labels.add(label);

        return label;
    }
}