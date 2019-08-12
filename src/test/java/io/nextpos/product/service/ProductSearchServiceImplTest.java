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

        productLabelService.createProductLabel(drinkLabel);
        productLabelService.createProductLabel(foodLabel);
        productLabelService.createProductLabel(labelWithoutProduct);

        final Product coffee = new Product(client, DummyObjects.dummyProductVersion("black coffee"));
        coffee.setProductLabel(drinkLabel);
        productService.createProduct(coffee);

        final Product appleJuice = new Product(client, DummyObjects.dummyProductVersion("apple juice"));
        appleJuice.setProductLabel(drinkLabel);
        productService.createProduct(appleJuice);

        final Product foodProduct = new Product(client, DummyObjects.dummyProductVersion());
        foodProduct.setProductLabel(foodLabel);
        productService.createProduct(foodProduct);

        final Product pasta = new Product(client, DummyObjects.dummyProductVersion("carbonara"));
        pasta.setProductLabel(pastaLabel);
        productService.createProduct(pasta);

        final Product productWithoutLabel = new Product(client, DummyObjects.dummyProductVersion("productWithoutLabel"));
        productService.createProduct(productWithoutLabel);
    }

    @Test
    void getAllProductsGroupedByLabels() {

        final Map<ProductLabel, List<ProductVersion>> products = productSearchService.getAllProductsGroupedByLabels(client, Version.DESIGN);

        assertThat(products).hasSize(5);
    }
}