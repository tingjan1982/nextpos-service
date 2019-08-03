package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.product.data.*;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@Transactional
class ProductOptionServiceImplTest {

    @Autowired
    private ProductOptionService productOptionService;

    @Autowired
    private ProductOptionVersionRepository productOptionVersionRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private ClientService clientService;

    @Test
    void createAndGetProductOption_OneChoice() {

        final Client client = createClient();

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("ice level", ProductOptionVersion.OptionType.ONE_CHOICE);
        stagingProductOption.addOptionValue("full");
        stagingProductOption.addOptionValue("3/4");
        stagingProductOption.addOptionValue("half");
        stagingProductOption.addOptionValue("1/3");
        stagingProductOption.addOptionValue("none");

        final ProductOption productOption = new ProductOption(client, stagingProductOption);

        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        verifyProductOption(createdProductOption);
        verifyProductOption(productOptionService.getProductOption(createdProductOption.getId()));
    }

    private void verifyProductOption(final ProductOption productOption) {
        assertThat(productOption.getId()).isNotNull();
        assertThat(productOption.getClient()).satisfies(c -> {
            assertThat(c).isNotNull();
            assertThat(c.getId()).isNotNull();
        });

        assertThat(productOption.getDesignVersion()).satisfies(staging -> {
            assertThat(staging.getId()).contains(productOption.getId());
            assertThat(staging.getOptionType()).isEqualTo(ProductOptionVersion.OptionType.ONE_CHOICE);
            assertThat(staging.getOptionValues()).hasSize(5);
        });
    }

    @Test
    void createAndGetProductOption_MultipleChoice() {

        final Client client = createClient();

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("extras", ProductOptionVersion.OptionType.MULTIPLE_CHOICE);
        stagingProductOption.addOptionValue("cheese");
        stagingProductOption.addOptionValue("egg");

        final ProductOption productOption = new ProductOption(client, stagingProductOption);

        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        assertThat(createdProductOption.getDesignVersion()).satisfies(staging -> {
            assertThat(staging.getId()).contains(productOption.getId());
            assertThat(staging.getOptionType()).isEqualTo(ProductOptionVersion.OptionType.MULTIPLE_CHOICE);
            assertThat(staging.getOptionValues()).hasSize(2);
        });
    }

    @Test
    void createAndGetProductOption_FreeText() {

        final Client client = createClient();

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("extras", ProductOptionVersion.OptionType.FREE_TEXT);
        stagingProductOption.addOptionValue("this shouldn't have any effect", BigDecimal.valueOf(50));

        final ProductOption productOption = new ProductOption(client, stagingProductOption);

        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        assertThat(createdProductOption.getDesignVersion()).satisfies(staging -> {
            assertThat(staging.getId()).contains(productOption.getId());
            assertThat(staging.getOptionType()).isEqualTo(ProductOptionVersion.OptionType.FREE_TEXT);
            assertThat(staging.getOptionValues()).isEmpty();
        });
    }

    @Test
    public void deployProductOption() {

        final Client client = createClient();

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("extras", ProductOptionVersion.OptionType.MULTIPLE_CHOICE);
        stagingProductOption.addOptionValue("cheese");
        stagingProductOption.addOptionValue("egg");

        final ProductOption productOption = new ProductOption(client, stagingProductOption);

        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        assertThat(createdProductOption.getDesignVersion()).isNotNull();
        assertThat(createdProductOption.getLiveVersion()).isNull();

        productOptionService.getProductOption(createdProductOption.getId());

        final ProductOption updated = productOptionService.deployProductOption(createdProductOption.getId());

        assertThat(updated.getLiveVersion()).satisfies(po -> {
            assertThat(po).isNotNull();
            assertThat(po.getId()).contains("-1"); // check that the version is incremented.
            assertThat(po.getVersion()).isEqualTo(Version.LIVE);
        });

        assertThat(updated.getDesignVersion()).satisfies(po -> {
            assertThat(po).isNotNull();
            assertThat(po.getId()).contains("-2"); // check that the version is incremented.
            assertThat(po.getVersion()).isEqualTo(Version.DESIGN);
        });

        productOptionService.deployProductOption(createdProductOption.getId());

        assertThat(productOptionVersionRepository.findAll()).hasSize(2);
    }

    @Test
    public void addProductOptionToProduct() {

        final Client client = createClient();

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("extras", ProductOptionVersion.OptionType.FREE_TEXT);
        final ProductOption productOption = new ProductOption(client, stagingProductOption);

        productOptionService.createProductOption(productOption);

        final Product product = new Product(client, DummyObjects.dummyProductVersion());

        productService.createProduct(product);

        final List<ProductOptionRelation> productOptionRelations = productOptionService.addProductOptionToProduct(productOption, Collections.singletonList(product));
        final ProductOptionRelation productOptionRelation = productOptionRelations.get(0);
        
        assertThat(productOptionRelation).isInstanceOf(ProductOptionRelation.ProductOptionOfProduct.class);
        assertThat(productOptionRelation).satisfies(r -> {
            assertThat(r.getId()).isNotNull();
            assertThat(r.getProductOption()).isEqualTo(productOption);
            assertThat(((ProductOptionRelation.ProductOptionOfProduct) r).getProduct()).isEqualTo(product);
        });

        final Product retrievedProduct = productService.getProduct(product.getId());
        assertThat(retrievedProduct.getProductOptionOfProducts()).hasSize(1);
    }

    private Client createClient() {
        final Client client = DummyObjects.dummyClient();
        clientService.createClient(client);
        return client;
    }
}