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
        final ProductLabel foodLabel = new ProductLabel("food", client);
        final ProductLabel pastaLabel = foodLabel.addChildProductLabel("pasta");
        final ProductLabel labelWithoutProduct = new ProductLabel("not used", client);

        productLabelService.saveProductLabel(drinkLabel);
        productLabelService.saveProductLabel(foodLabel);
        productLabelService.saveProductLabel(labelWithoutProduct);

        final Product coffee = new Product(client, DummyObjects.dummyProductVersion("black coffee"));
        coffee.setProductLabel(drinkLabel);
        coffee.setPinned(true);
        productService.saveProduct(coffee);

        final Product appleJuice = new Product(client, DummyObjects.dummyProductVersion("apple juice"));
        appleJuice.setProductLabel(drinkLabel);
        productService.saveProduct(appleJuice);

        final Product foodProduct = new Product(client, DummyObjects.dummyProductVersion());
        foodProduct.setProductLabel(foodLabel);
        productService.saveProduct(foodProduct);

        final Product pasta = new Product(client, DummyObjects.dummyProductVersion("carbonara"));
        pasta.setProductLabel(pastaLabel);
        productService.saveProduct(pasta);

        final Product productWithoutLabel = new Product(client, DummyObjects.dummyProductVersion("productWithoutLabel"));
        productService.saveProduct(productWithoutLabel);
    }

    @Test
    void getAllProductsGroupedByLabels() {

        final Map<ProductLabel, List<ProductVersion>> products = productSearchService.getAllProductsGroupedByLabels(client, Version.DESIGN);

        assertThat(products).hasSize(6);
        assertThat(findProductsByLabel(products, "drink")).hasSize(2);
        assertThat(findProductsByLabel(products, "ungrouped")).hasSize(1);
        assertThat(findProductsByLabel(products, "pinned")).hasSize(1);
    }

    private List<ProductVersion> findProductsByLabel(Map<ProductLabel, List<ProductVersion>> products, String label) {

        return products.entrySet().stream()
                .filter(entry -> label.equals(entry.getKey().getName()))
                .map(Map.Entry::getValue)
                .findFirst().orElseThrow();
    }
}