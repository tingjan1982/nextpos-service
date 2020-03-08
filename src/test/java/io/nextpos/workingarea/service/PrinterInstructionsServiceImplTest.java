package io.nextpos.workingarea.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.ElectronicInvoice;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.util.InvoiceQRCodeEncryptor;
import io.nextpos.shared.DummyObjects;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.PrinterInstructions;
import io.nextpos.workingarea.data.WorkingArea;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
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
        clientRepository.save(client);

        workingArea = new WorkingArea(client, "main");
        workingArea.setNoOfPrintCopies(1);
        workingArea.addPrinter(new Printer(client, "main printer", "192.168.1.100", Printer.ServiceType.WORKING_AREA));
        workingAreaService.saveWorkingArea(workingArea);
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

        System.out.println(orderToWorkingArea);
    }


    @Test
    void createOrderDetailsPrintInstruction() {

        final Client client = DummyObjects.dummyClient();
        client.addAttribute(Client.ClientAttributes.UBN.name(), "22640971");
        client.addAttribute(Client.ClientAttributes.ADDRESS.name(), "台北市大安區建國南路二段");
        final OrderTransaction orderTransaction = new OrderTransaction(new ObjectId().toString(), client.getId(), BigDecimal.valueOf(150), BigDecimal.valueOf(150),
                OrderTransaction.PaymentMethod.CARD,
                OrderTransaction.BillType.SINGLE,
                List.of(new OrderTransaction.BillLineItem("coffee", 1, BigDecimal.valueOf(55)),
                        new OrderTransaction.BillLineItem("bagel", 2, BigDecimal.valueOf(70))));

        orderTransaction.setId("dummy-id");
        orderTransaction.setCreatedDate(new Date());

        final String instruction = printerInstructionService.createOrderDetailsPrintInstruction(client, orderTransaction);

        LOGGER.info("{}", instruction);
    }

    @Test
    void createElectronicInvoiceXML() throws Exception {

        final Client client = DummyObjects.dummyClient();
        client.addAttribute(Client.ClientAttributes.UBN.name(), "22640971");
        client.addAttribute(Client.ClientAttributes.ADDRESS.name(), "台北市大安區建國南路二段");

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot("coffee", BigDecimal.valueOf(55)), 1);
        order.addOrderLineItem(DummyObjects.productSnapshot("bagel", BigDecimal.valueOf(70)), 2);

        ElectronicInvoice electronicInvoice = new ElectronicInvoice(
                order.getId(),
                "inv-1001",
                "1",
                "AA00000001",
                new ElectronicInvoice.InvoicePeriod("2020", "3", "6"),
                "4567",
                "2020-03-06 00:00:00AM",
                order.getTotal().getAmountWithoutTax(),
                order.getTotal().getTax(),
                "22640971");

        electronicInvoice.generateBarcodeContent();

        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        final String aesKey = Base64.getEncoder().encodeToString(salt);
        
        final InvoiceQRCodeEncryptor invoiceQRCodeEncryptor = new InvoiceQRCodeEncryptor("12341234123412341234123412341234");
        electronicInvoice.generateQrCode1Content(invoiceQRCodeEncryptor, order);
        electronicInvoice.generateQrCode2Content(order);

        final OrderTransaction orderTransaction = new OrderTransaction(new ObjectId().toString(), client.getId(), BigDecimal.valueOf(150), BigDecimal.valueOf(150),
                OrderTransaction.PaymentMethod.CARD,
                OrderTransaction.BillType.SINGLE,
                List.of(new OrderTransaction.BillLineItem("coffee", 1, BigDecimal.valueOf(55)),
                        new OrderTransaction.BillLineItem("bagel", 2, BigDecimal.valueOf(70))));

        orderTransaction.setId("dummy-id");
        orderTransaction.setCreatedDate(new Date());
        orderTransaction.getInvoiceDetails().setElectronicInvoice(electronicInvoice);

        final String instruction = printerInstructionService.createElectronicInvoiceXML(client, order, orderTransaction);

        LOGGER.info("{}", instruction);
    }
}