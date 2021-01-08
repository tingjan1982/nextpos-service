package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductRepository;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class ClientBootstrapServiceImplTest {

    private final ClientService clientService;

    private final ClientBootstrapService clientBootstrapService;

    private final TableLayoutService tableLayoutService;

    private final WorkingAreaService workingAreaService;

    private final ProductLabelService productLabelService;

    private final ProductRepository productRepository;

    @Autowired
    ClientBootstrapServiceImplTest(ClientService clientService, ClientBootstrapService clientBootstrapService, TableLayoutService tableLayoutService, WorkingAreaService workingAreaService, ProductLabelService productLabelService, ProductRepository productRepository) {
        this.clientService = clientService;
        this.clientBootstrapService = clientBootstrapService;
        this.tableLayoutService = tableLayoutService;
        this.workingAreaService = workingAreaService;
        this.productLabelService = productLabelService;
        this.productRepository = productRepository;
    }

    @Test
    void bootstrapClient() {

        final Client client = DummyObjects.dummyClient();
        clientService.saveClient(client);

        clientBootstrapService.bootstrapClient(client);

        final List<TableLayout> tableLayouts = tableLayoutService.getTableLayouts(client);

        assertThat(tableLayouts).hasSize(1);
        assertThat(tableLayouts.get(0).getTables()).hasSize(3);

        assertThat(workingAreaService.getWorkingAreas(client)).hasSize(1);

        assertThat(productLabelService.getProductLabels(client)).hasSize(3);

        final List<Product> products = productRepository.findAll();

        assertThat(products).hasSize(3);
        assertThat(products).allSatisfy(p -> {
            assertThat(p.getProductLabel()).isNotNull();
            assertThat(p.getWorkingArea()).isNotNull();
        });
    }
}