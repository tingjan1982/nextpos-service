package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.product.data.ProductOption;
import io.nextpos.product.data.ProductOptionVersion;
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
class ProductOptionServiceImplTest {

    @Autowired
    private ProductOptionService productOptionService;

    @Autowired
    private ClientService clientService;

    @Test
    void createAndGetProductOption_OneChoice() {

        final Client client = new Client("test", "test", "test");
        clientService.createClient(client);
        
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

        assertThat(productOption.getStagingProductOption()).satisfies(staging -> {
            assertThat(staging.getId()).contains(productOption.getId());
            assertThat(staging.getOptionType()).isEqualTo(ProductOptionVersion.OptionType.ONE_CHOICE);
            assertThat(staging.getOptionValues()).hasSize(5);
        });
    }

    @Test
    void createAndGetProductOption_MultipleChoice() {

        final Client client = new Client("test", "test", "test");
        clientService.createClient(client);

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("extras", ProductOptionVersion.OptionType.MULTIPLE_CHOICE);
        stagingProductOption.addOptionValue("cheese");
        stagingProductOption.addOptionValue("egg");

        final ProductOption productOption = new ProductOption(client, stagingProductOption);

        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        assertThat(createdProductOption.getStagingProductOption()).satisfies(staging -> {
            assertThat(staging.getId()).contains(productOption.getId());
            assertThat(staging.getOptionType()).isEqualTo(ProductOptionVersion.OptionType.MULTIPLE_CHOICE);
            assertThat(staging.getOptionValues()).hasSize(2);
        });
    }

    @Test
    void createAndGetProductOption_FreeText() {

        final Client client = new Client("test", "test", "test");
        clientService.createClient(client);

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("extras", ProductOptionVersion.OptionType.FREE_TEXT);
        stagingProductOption.addOptionValue("this shouldn't have any effect", BigDecimal.valueOf(50));

        final ProductOption productOption = new ProductOption(client, stagingProductOption);

        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        assertThat(createdProductOption.getStagingProductOption()).satisfies(staging -> {
            assertThat(staging.getId()).contains(productOption.getId());
            assertThat(staging.getOptionType()).isEqualTo(ProductOptionVersion.OptionType.FREE_TEXT);
            assertThat(staging.getOptionValues()).isEmpty();
        });
    }
}