package io.nextpos.workingarea.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeRepository;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.service.ElectronicInvoiceService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.PrinterInstructions;
import io.nextpos.workingarea.data.WorkingArea;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@SpringBootTest
@Transactional
class PrinterInstructionsServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrinterInstructionsServiceImplTest.class);

    @Autowired
    private PrinterInstructionService printerInstructionService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ElectronicInvoiceService electronicInvoiceService;

    @Autowired
    private InvoiceNumberRangeService invoiceNumberRangeService;

    @Autowired
    private InvoiceNumberRangeRepository invoiceNumberRangeRepository;

    @Autowired
    private WorkingAreaService workingAreaService;

    @Autowired
    private OrderSettings orderSettings;

    @Autowired
    private ClientRepository clientRepository;

    private Client client;

    private WorkingArea workingArea;

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        final String ubn = "83515813";
        client.addAttribute(Client.ClientAttributes.UBN.name(), ubn);
        client.addAttribute(Client.ClientAttributes.ADDRESS.name(), "台北市大安區建國南路二段");
        client.addAttribute(Client.ClientAttributes.AES_KEY.name(), "41BFE9D500D25491650E8B84C3EA3B3C");

        clientRepository.save(client);

        final Printer printer = new Printer(client, "main printer", "192.168.1.100", Printer.ServiceType.WORKING_AREA);
        workingAreaService.savePrinter(printer);

        workingArea = new WorkingArea(client, "main");
        workingArea.setNoOfPrintCopies(1);
        workingArea.addPrinter(printer);
        workingAreaService.saveWorkingArea(workingArea);

        InvoiceNumberRange invoiceNumberRange = new InvoiceNumberRange(ubn, "1090910", "AG", "00000001", "10000001");
        invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);
    }

    @AfterEach
    void teardown() {
        invoiceNumberRangeRepository.deleteAll();
    }

    @Test
    void createOrderToWorkingArea() {

        final Order order = new Order(client.getId(), orderSettings);

        final OrderLineItem item1 = new OrderLineItem(DummyObjects.productSnapshot(), 2, orderSettings);
        item1.setState(OrderLineItem.LineItemState.IN_PROCESS);
        item1.setWorkingAreaId(workingArea.getId());
        order.addOrderLineItem(item1);

        orderService.createOrder(order);

        final PrinterInstructions orderToWorkingArea = printerInstructionService.createOrderToWorkingArea(order);

        LOGGER.info("{}", orderToWorkingArea);
    }


    @Test
    void createOrderDetailsPrintInstruction() {

        final OrderTransaction orderTransaction = new OrderTransaction(new ObjectId().toString(), client.getId(), BigDecimal.valueOf(195), BigDecimal.valueOf(195),
                OrderTransaction.PaymentMethod.CARD,
                OrderTransaction.BillType.SINGLE,
                List.of(new OrderTransaction.BillLineItem("coffee", 1, BigDecimal.valueOf(55), BigDecimal.valueOf(55)),
                        new OrderTransaction.BillLineItem("bagel", 2, BigDecimal.valueOf(70), BigDecimal.valueOf(140))));

        orderTransaction.setId("dummy-id");
        orderTransaction.setCreatedDate(new Date());

        final String instruction = printerInstructionService.createOrderDetailsPrintInstruction(client, orderTransaction);

        LOGGER.info("{}", instruction);
    }

    @Test
    void createElectronicInvoiceXML() throws Exception {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot("coffee", BigDecimal.valueOf(55)), 1);
        order.addOrderLineItem(DummyObjects.productSnapshot("bagel", BigDecimal.valueOf(70)), 2);

        final OrderTransaction orderTransaction = new OrderTransaction(new ObjectId().toString(), client.getId(), BigDecimal.valueOf(150), BigDecimal.valueOf(150),
                OrderTransaction.PaymentMethod.CARD,
                OrderTransaction.BillType.SINGLE,
                List.of(new OrderTransaction.BillLineItem("coffee", 1, BigDecimal.valueOf(50), BigDecimal.valueOf(50)),
                        new OrderTransaction.BillLineItem("bagel", 2, BigDecimal.valueOf(50), BigDecimal.valueOf(100))));

        orderTransaction.setId("dummy-id");
        orderTransaction.setCreatedDate(new Date());

        final ElectronicInvoice electronicInvoice = electronicInvoiceService.createElectronicInvoice(client, order, orderTransaction);

        orderTransaction.getInvoiceDetails().setElectronicInvoice(electronicInvoice);

        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        final String aesKey = Base64.getEncoder().encodeToString(salt);
        
        final String instruction = printerInstructionService.createElectronicInvoiceXML(client, order, orderTransaction);

        LOGGER.info("{}", instruction);
    }
}