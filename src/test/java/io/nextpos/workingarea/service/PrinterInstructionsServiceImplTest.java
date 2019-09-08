package io.nextpos.workingarea.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.PrinterInstructions;
import io.nextpos.workingarea.data.WorkingArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PrinterInstructionsServiceImplTest {

    @Autowired
    private PrinterInstructionService printerInstructionService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WorkingAreaService workingAreaService;

    @Autowired
    private CountrySettings countrySettings;

    @Autowired
    private ClientRepository clientRepository;

    private Client client;

    private WorkingArea workingArea;

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientRepository.save(client);

        workingArea = new WorkingArea(client, "main");
        workingArea.setNoOfPrintCopies(1);
        workingArea.addPrinter(new Printer(client, "main printer", "192.168.1.100", Printer.ServiceType.WORKING_AREA));
        workingAreaService.saveWorkingArea(workingArea);
    }

    @Test
    void createOrderToWorkingArea() {

        final Order order = new Order(client.getId(), countrySettings.getTaxRate(), countrySettings.getCurrency());

        final OrderLineItem item1 = new OrderLineItem(DummyObjects.productSnapshot(), 2, countrySettings.getTaxRate());
        item1.setState(Order.OrderState.IN_PROCESS);
        item1.setWorkingAreaId(workingArea.getId());
        order.addOrderLineItem(item1);

        orderService.createOrder(order);

        final PrinterInstructions orderToWorkingArea = printerInstructionService.createOrderToWorkingArea(order);

        System.out.println(orderToWorkingArea);
    }
}