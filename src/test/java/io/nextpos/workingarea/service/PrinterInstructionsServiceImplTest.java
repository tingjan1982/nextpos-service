package io.nextpos.workingarea.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeRepository;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.service.ElectronicInvoiceService;
import io.nextpos.settings.data.CountrySettings;
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
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

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
    private CountrySettings countrySettings;

    @Autowired
    private OrderSettings orderSettings;

    @Autowired
    private ClientService clientService;

    private Client client;

    private WorkingArea workingArea;

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        client.setClientName("Rain");
        final String ubn = "83515813";
        client.addAttribute(Client.ClientAttributes.UBN, ubn);
        client.addAttribute(Client.ClientAttributes.COMPANY_NAME, "雨圖數位行銷科技股份有限公司");
        client.addAttribute(Client.ClientAttributes.ADDRESS, "台北市大安區建國南路二段");
        client.addAttribute(Client.ClientAttributes.AES_KEY, "41BFE9D500D25491650E8B84C3EA3B3C");

        clientService.saveClient(client);

        final Printer printer = new Printer(client, "main printer", "192.168.2.231", Set.of(Printer.ServiceType.WORKING_AREA));
        workingAreaService.savePrinter(printer);

        workingArea = new WorkingArea(client, "main");
        workingArea.setNoOfPrintCopies(1);
        workingArea.addPrinter(printer);
        workingAreaService.saveWorkingArea(workingArea);

        InvoiceNumberRange invoiceNumberRange = new InvoiceNumberRange(ubn, invoiceNumberRangeService.getCurrentRangeIdentifier(), "AG", "10000006", "10000099");
        invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);
    }

    @AfterEach
    void teardown() {
        invoiceNumberRangeRepository.deleteAll();
    }

    @Test
    void createOrderToWorkingArea() {

        final Order order = new Order(client.getId(), orderSettings);
        ProductSnapshot.ProductOptionSnapshot options = new ProductSnapshot.ProductOptionSnapshot("註記", "後上後上後上後上");
        final ProductSnapshot coffeeP = DummyObjects.productSnapshot("義大利經典鹽烤杏齙菇", new BigDecimal("100"), options);
        coffeeP.setChildProducts(Arrays.asList(
                new ProductSnapshot.ChildProductSnapshot("id", "sugar", null),
                new ProductSnapshot.ChildProductSnapshot("id", "ice", "ICE")
        ));
        final OrderLineItem coffee = order.addOrderLineItem(coffeeP, 5);
        coffee.setState(OrderLineItem.LineItemState.IN_PROCESS);
        coffee.setWorkingAreaId(workingArea.getId());

        final OrderLineItem friedRice = order.addOrderLineItem(DummyObjects.productSnapshot("蛋炒飯", new BigDecimal("100"), DummyObjects.productOptionSnapshot()), 5);
        friedRice.setState(OrderLineItem.LineItemState.IN_PROCESS);
        friedRice.setWorkingAreaId(workingArea.getId());

        orderService.createOrder(order);

        final PrinterInstructions orderToWorkingArea = printerInstructionService.createOrderToWorkingArea(order);

        LOGGER.info("{}", orderToWorkingArea);

        printInstruction(orderToWorkingArea.getPrinterInstructions().get(0).getPrintInstruction());
    }

    @Test
    void createOrderDetailsPrintInstruction() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot("coffee", new BigDecimal("55")), 3);
        order.addOrderLineItem(DummyObjects.productSnapshot("apple juice", new BigDecimal("70")), 1);
        order.addOrderLineItem(DummyObjects.productSnapshot("什錦炒麵", new BigDecimal("120")), 2);
        order.addOrderLineItem(DummyObjects.productSnapshot("菲力牛排", new BigDecimal("550")), 1);
        order.addOrderLineItem(DummyObjects.productSnapshot("義大利麵", new BigDecimal("320")), 1);
        orderService.saveOrder(order);

        final OrderTransaction orderTransaction = new OrderTransaction(order,
                OrderTransaction.PaymentMethod.CARD,
                OrderTransaction.BillType.SINGLE,
                order.getOrderTotal());

        orderTransaction.setId(ObjectId.get().toString());
        orderTransaction.setOrderId(order.getId());
        orderTransaction.setCreatedDate(new Date());

        final String instruction = printerInstructionService.createOrderDetailsPrintInstruction(client, order, orderTransaction);

        logAndPrint(instruction);

        final String cancelInstruction = printerInstructionService.createCancelOrderPrintInstruction(client, order, orderTransaction);

        logAndPrint(cancelInstruction);
    }

    @Test
    void createCancelOrderPrintInstruction() {

        final OrderSettings orderSettings = new OrderSettings(countrySettings, true, BigDecimal.ZERO);
        final Order order = new Order(client.getId(), orderSettings);
        final String serialId = orderService.generateSerialId(client.getId());
        order.setSerialId(serialId);
        order.addOrderLineItem(DummyObjects.productSnapshot("美式", new BigDecimal("100")), 1);
        orderService.saveOrder(order);

        final OrderTransaction orderTransaction = new OrderTransaction(order,
                OrderTransaction.PaymentMethod.CARD,
                OrderTransaction.BillType.SINGLE,
                order.getOrderTotal());

        orderTransaction.setId(ObjectId.get().toString());
        orderTransaction.setOrderId(order.getId());
        orderTransaction.setCreatedDate(new Date());
        orderTransaction.updateInvoiceDetails("83515813", null, null, null, false);

        final ElectronicInvoice electronicInvoice = electronicInvoiceService.createElectronicInvoice(client, order, orderTransaction);
        electronicInvoice.setBuyerName("雨圖數位行銷科技股份有限公司");
        orderTransaction.getInvoiceDetails().setElectronicInvoice(electronicInvoice);

        final String cancelInstruction = printerInstructionService.createCancelOrderPrintInstruction(client, order, orderTransaction);

        logAndPrint(cancelInstruction);
    }

    @Test
    void createElectronicInvoiceXML() {

        final OrderSettings orderSettings = new OrderSettings(countrySettings, true, BigDecimal.ZERO);
        final Order order = new Order(client.getId(), orderSettings);
        order.setSerialId("20201010-150");
        order.addOrderLineItem(DummyObjects.productSnapshot("coffee", BigDecimal.valueOf(55)), 3);
//        order.addOrderLineItem(DummyObjects.productSnapshot("bagel", BigDecimal.valueOf(70)), 2);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式1", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式2", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式3", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式4", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式5", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式6", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式7", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式8", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式9", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式10", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式11", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式12", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式13", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式14", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式15", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式", BigDecimal.valueOf(45)), 1);
//        order.addOrderLineItem(DummyObjects.productSnapshot("黑美式", BigDecimal.valueOf(45)), 1);

        orderService.saveOrder(order);

        final OrderTransaction orderTransaction = new OrderTransaction(order,
                OrderTransaction.PaymentMethod.CARD,
                OrderTransaction.BillType.SINGLE,
                order.getOrderTotal());

        orderTransaction.setId(ObjectId.get().toString());
        orderTransaction.setCreatedDate(new Date());
        orderTransaction.updateInvoiceDetails("27252210", null, null, null, false);

        final ElectronicInvoice electronicInvoice = electronicInvoiceService.createElectronicInvoice(client, order, orderTransaction);

        orderTransaction.getInvoiceDetails().setElectronicInvoice(electronicInvoice);

        final String instruction = printerInstructionService.createElectronicInvoiceXML(client, order, orderTransaction, false);

        logAndPrint(instruction);
    }

    private void logAndPrint(String instruction) {
        LOGGER.info("{}", instruction);

        printInstruction(instruction);
    }

    private void printInstruction(String printInstruction) {
        printerInstructionService.outputToPrinter("192.168.2.246", printInstruction);
    }
}