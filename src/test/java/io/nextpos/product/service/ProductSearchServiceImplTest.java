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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        createProduct(ProductBuilder.builder(client).productName("frappe").sku("coffee"));
        createProduct(ProductBuilder.builder(client).productName("latte").description("a type of coffee"));

        final Product productWithoutLabel = createProduct("productWithoutLabel", null);
    }

    private Product createProduct(String productName, ProductLabel productLabel) {
        return createProduct(productName, productLabel, false);
    }

    private Product createProduct(String productName, ProductLabel productLabel, boolean pinned) {

        final ProductBuilder builder = ProductBuilder.builder(client)
                .productName(productName)
                .productLabel(productLabel)
                .pinned(pinned);

        return this.createProduct(builder);
    }

    private Product createProduct(ProductBuilder productBuilder) {
        return productService.saveProduct(productBuilder.build());
    }

    private static class ProductBuilder {

        private final Client client;

        private String productName = "dummy";

        private String sku = "sku";

        private String description = "description";

        private ProductLabel productLabel;

        private boolean pinned;

        public ProductBuilder(final Client client) {
            this.client = client;
        }

        static ProductBuilder builder(Client client) {
            return new ProductBuilder(client);
        }

        ProductBuilder productName(String productName) {
            this.productName = productName;
            return this;
        }

        ProductBuilder sku(String sku) {
            this.sku = sku;
            return this;
        }

        ProductBuilder description(String description) {
            this.description = description;
            return this;
        }

        ProductBuilder productLabel(ProductLabel productLabel) {
            this.productLabel = productLabel;
            return this;
        }

        ProductBuilder pinned(boolean pinned) {
            this.pinned = pinned;
            return this;
        }

        Product build() {
            final ProductVersion productVersion = new ProductVersion(productName, sku, description, BigDecimal.ZERO);
            final Product product = new Product(client, productVersion);

            if (productLabel != null) {
                product.setProductLabel(productLabel);
            }

            product.setPinned(pinned);

            return product;
        }
    }

    @Test
    void getAllProductsGroupedByLabels() {

        final Map<ProductLabel, List<ProductVersion>> products = productSearchService.getAllProductsGroupedByLabels(client, Version.DESIGN);

        assertThat(products).hasSize(6);
        assertThat(findProductsByLabel(products, "drink")).hasSize(3);
        assertThat(findProductsByLabel(products, "drink")).isSortedAccordingTo(Comparator.comparing(ProductVersion::getProductName));
        assertThat(findProductsByLabel(products, "ungrouped")).hasSize(3);
        assertThat(findProductsByLabel(products, "pinned")).hasSize(2);
    }

    @Test
    void getProductsByKeyword() {

        final List<ProductVersion> products = productSearchService.getProductsByKeyword(client, Version.DESIGN, "COFFEE");

        assertThat(products).hasSize(4);
    }

    @Test
    void getProductsByKeyword_EmptyKeyword() {

        final List<ProductVersion> products = productSearchService.getProductsByKeyword(client, Version.DESIGN, "");

        assertThat(products).isEmpty();
    }

    private List<ProductVersion> findProductsByLabel(Map<ProductLabel, List<ProductVersion>> products, String label) {

        return products.entrySet().stream()
                .filter(entry -> label.equals(entry.getKey().getName()))
                .map(Map.Entry::getValue)
                .findFirst().orElseThrow();
    }
}