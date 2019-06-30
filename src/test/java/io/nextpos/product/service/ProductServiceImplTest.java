package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.shared.model.VersionableClientObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@Transactional
class ProductServiceImplTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ClientRepository clientRepository;


    @Test
    void createAndGetProduct() {

        final Client client = new Client("Ron", "user", "password");
        final Client createdClient = clientRepository.save(client);

        final ProductVersion productVersion = new ProductVersion("Gin Topic", "sku-001", "signature drink", BigDecimal.valueOf(350));
        final Product product = new Product(createdClient);
        product.addNewVersion(productVersion);

        final Product createdProduct = productService.createProduct(product);

        assertProduct(createdProduct, product);

        final Product existingProduct = productService.getProduct(createdProduct.getId());

        assertProduct(existingProduct, product);
    }

    private void assertProduct(Product actual, Product expected) {

        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getClient().getId()).isNotNull();
        assertThat(actual.getLatestVersion().getProductName()).isEqualTo(expected.getLatestVersion().getProductName());
        assertThat(actual.getLatestVersion().getSku()).isEqualTo(expected.getLatestVersion().getSku());
        assertThat(actual.getLatestVersion().getPrice()).isEqualTo(expected.getLatestVersion().getPrice());
        assertThat(actual.getLatestVersion().getState()).isEqualTo(VersionableClientObject.ObjectState.DESIGN);
        assertThat(actual.getLatestVersion().getVersion()).isEqualTo(1);
        assertThat(actual.getCreatedTime()).isNotNull();
        assertThat(actual.getUpdatedTime()).isNotNull();

    }

    @Test
    void deleteProduct() {


    }
}