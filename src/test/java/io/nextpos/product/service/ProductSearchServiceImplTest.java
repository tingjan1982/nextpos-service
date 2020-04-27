package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.product.data.Version;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductSearchServiceImplTest {

    @Autowired
    private ProductSearchService productSearchService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductLabelService productLabelService;

    private Client client;

    @BeforeEach
    public void prepareData() {

        client = clientService.createClient(DummyObjects.dummyClient());
        final ProductLabel drinkLabel = new ProductLabel("drink", client);
        drinkLabel.setOrderKey("2");
        final ProductLabel foodLabel = new ProductLabel("food", client);
        foodLabel.setOrderKey("1");
        final ProductLabel pastaLabel = foodLabel.addChildProductLabel("pasta");
        final ProductLabel labelWithoutProduct = new ProductLabel("not used", client);

        productLabelService.saveProductLabel(drinkLabel);
        productLabelService.saveProductLabel(foodLabel);
        productLabelService.saveProductLabel(labelWithoutProduct);

        final Product coffee = createProduct("black coffee", drinkLabel, true);
        final Product latte = createProduct("latte coffee", drinkLabel, true);
        final Product appleJuice = createProduct("apple juice", drinkLabel);

        final Product foodProduct = createProduct("salad", foodLabel);
        final Product pasta = createProduct("carbonara", pastaLabel);

        final Product productWithoutLabel = createProduct("productWithoutLabel", null);
    }

    private Product createProduct(String productName, ProductLabel productLabel) {
        return createProduct(productName, productLabel, false);
    }

    private Product createProduct(String productName, ProductLabel productLabel, boolean pinned) {

        final Product product = new Product(client, DummyObjects.dummyProductVersion(productName));

        if (productLabel != null) {
            product.setProductLabel(productLabel);
        }

        product.setPinned(pinned);

        return productService.saveProduct(product);
    }

    @Test
    void getAllProductsGroupedByLabels() {

        final Map<ProductLabel, List<ProductVersion>> products = productSearchService.getAllProductsGroupedByLabels(client, Version.DESIGN);

        assertThat(products).hasSize(6);
        assertThat(findProductsByLabel(products, "drink")).hasSize(3);
        assertThat(findProductsByLabel(products, "drink")).isSortedAccordingTo(Comparator.comparing(ProductVersion::getProductName));
        assertThat(findProductsByLabel(products, "ungrouped")).hasSize(1);
        assertThat(findProductsByLabel(products, "pinned")).hasSize(2);
    }

    private List<ProductVersion> findProductsByLabel(Map<ProductLabel, List<ProductVersion>> products, String label) {

        return products.entrySet().stream()
                .filter(entry -> label.equals(entry.getKey().getName()))
                .map(Map.Entry::getValue)
                .findFirst().orElseThrow();
    }
}