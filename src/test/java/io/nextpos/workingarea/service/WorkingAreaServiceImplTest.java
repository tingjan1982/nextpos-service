package io.nextpos.workingarea.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductLabelRepository;
import io.nextpos.product.data.ProductRepository;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.WorkingArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ChainedTransaction
class WorkingAreaServiceImplTest {

    @Autowired
    private WorkingAreaService workingAreaService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProductLabelRepository productLabelRepository;

    @Autowired
    private ProductRepository productRepository;

    private Client client;

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientService.saveClient(client);
    }

    @Test
    void crudWorkingArea() {

        final Printer printer = new Printer(client, "main", "192.168.1.125", Set.of(Printer.ServiceType.WORKING_AREA));
        workingAreaService.savePrinter(printer);

        final WorkingArea bar = new WorkingArea(client, "bar");
        bar.addPrinter(printer);
        final WorkingArea savedBar = workingAreaService.saveWorkingArea(bar);

        assertThat(savedBar.getId()).isNotNull();
        assertThat(savedBar.getClient()).isEqualTo(client);
        assertThat(savedBar.getName()).isEqualTo("bar");
        assertThat(savedBar.getPrinters()).hasSize(1);
        assertThat(printer.getWorkingAreas()).hasSize(1);
        assertThatCode(() -> workingAreaService.getWorkingArea(savedBar.getId())).doesNotThrowAnyException();

        final WorkingArea outside = new WorkingArea(client, "outside");
        outside.setVisibility(WorkingArea.Visibility.ROSTER);
        workingAreaService.saveWorkingArea(outside);

        final WorkingArea kitchen = new WorkingArea(client, "kitchen");
        kitchen.setVisibility(WorkingArea.Visibility.PRODUCT);
        workingAreaService.saveWorkingArea(kitchen);

        assertThat(workingAreaService.getWorkingAreas(client)).hasSize(3);
        assertThat(workingAreaService.getWorkingAreas(client, WorkingArea.Visibility.ALL)).hasSize(3);
        assertThat(workingAreaService.getWorkingAreas(client, WorkingArea.Visibility.PRODUCT)).hasSize(1);
        assertThat(workingAreaService.getWorkingAreas(client, WorkingArea.Visibility.ROSTER)).hasSize(1);

        final Product product = Product.builder(client).productNameAndPrice("p1", new BigDecimal("100")).build();
        product.setWorkingArea(savedBar);
        productRepository.save(product);

        final ProductLabel label = new ProductLabel("label", client);
        label.setWorkingArea(savedBar);
        productLabelRepository.save(label);

        assertThatThrownBy(() -> workingAreaService.deleteWorkingArea(savedBar)).isInstanceOf(BusinessLogicException.class);

        productLabelRepository.delete(label);

        assertThatThrownBy(() -> workingAreaService.deleteWorkingArea(savedBar)).isInstanceOf(BusinessLogicException.class);

        productRepository.delete(product);

        assertThatCode(() -> workingAreaService.deleteWorkingArea(savedBar)).doesNotThrowAnyException();

        assertThat(workingAreaService.getPrinter(printer.getId()).getWorkingAreas()).isEmpty();
    }

    @Test
        //@Rollback(false) this doesn't really work to test unique constraint as the commit happens after the method execution with no chance to handle.
    void crudPrinter() {
        final Printer registerPrinter = new Printer(client, "register", "192.168.1.125", Set.of(Printer.ServiceType.CHECKOUT));
        final Printer savedPrinter = workingAreaService.savePrinter(registerPrinter);

        assertThat(savedPrinter.getId()).isNotNull();
        assertThat(savedPrinter.getClient()).isEqualTo(client);
        assertThat(savedPrinter.getServiceTypes()).isNotEmpty();

        assertThatCode(() -> workingAreaService.getPrinter(savedPrinter.getId())).doesNotThrowAnyException();

        assertThat(workingAreaService.getPrinters(client)).hasSize(1);

        assertThat(workingAreaService.getPrintersByServiceType(client, Printer.ServiceType.CHECKOUT)).hasSize(1);
    }
}