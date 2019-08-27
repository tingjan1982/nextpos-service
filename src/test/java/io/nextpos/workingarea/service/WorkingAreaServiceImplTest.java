package io.nextpos.workingarea.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.shared.DummyObjects;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.WorkingArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@Transactional
class WorkingAreaServiceImplTest {

    @Autowired
    private WorkingAreaService workingAreaService;

    @Autowired
    private ClientRepository clientRepository;

    private Client client;

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientRepository.save(client);
    }

    @Test
    void crudWorkingArea() {
        final WorkingArea bar = new WorkingArea(client, "bar");
        final Printer printer = new Printer(client, "main", "192.168.1.125", Printer.ServiceType.WORKING_AREA);
        bar.addPrinter(printer);
        final WorkingArea savedBar = workingAreaService.saveWorkingArea(bar);

        assertThat(savedBar.getId()).isNotNull();
        assertThat(savedBar.getClient()).isEqualTo(client);
        assertThat(savedBar.getName()).isEqualTo("bar");
        assertThat(savedBar.getPrinters()).hasSize(1);

        assertThatCode(() -> workingAreaService.getWorkingArea(savedBar.getId())).doesNotThrowAnyException();

        assertThat(workingAreaService.getWorkingAreas(client)).hasSize(1);
    }

    @Test
        //@Rollback(false) this doesn't really work to test unique constraint as the commit happens after the method execution with no chance to handle.
    void crudPrinter() {
        final Printer registerPrinter = new Printer(client, "register", "192.168.1.125", Printer.ServiceType.CHECKOUT);

        final Printer savedPrinter = workingAreaService.savePrinter(registerPrinter);

        assertThat(savedPrinter.getId()).isNotNull();
        assertThat(savedPrinter.getClient()).isEqualTo(client);
        assertThat(savedPrinter.getServiceType()).isEqualTo(Printer.ServiceType.CHECKOUT);

        assertThatCode(() -> workingAreaService.getPrinter(savedPrinter.getId())).doesNotThrowAnyException();

        assertThat(workingAreaService.getPrinters(client)).hasSize(1);
    }
}