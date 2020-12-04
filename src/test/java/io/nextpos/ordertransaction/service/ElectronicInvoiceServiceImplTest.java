package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueService;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class ElectronicInvoiceServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElectronicInvoiceServiceImplTest.class);

    @Autowired
    private ElectronicInvoiceService electronicInvoiceService;

    @Autowired
    private InvoiceNumberRangeService invoiceNumberRangeService;

    @Autowired
    private PendingEInvoiceQueueService pendingEInvoiceQueueService;

    @Autowired
    private OrderSettings orderSettings;

    private Client client;

    private final String ubn = "83515813";

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        client.addAttribute(Client.ClientAttributes.UBN.name(), ubn);
        client.addAttribute(Client.ClientAttributes.AES_KEY.name(), "12341234123412341234123412341234");
    }
    
    @Test
    void createElectronicInvoice() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 2);

        final OrderTransaction orderTransaction = new OrderTransaction(order.getId(), client.getId(), order.getOrderTotal(), order.getOrderTotal(), OrderTransaction.PaymentMethod.CARD, OrderTransaction.BillType.SINGLE, List.of());
        orderTransaction.putPaymentDetails(OrderTransaction.PaymentDetailsKey.LAST_FOUR_DIGITS, "1234");

        assertThat(electronicInvoiceService.checkElectronicInvoiceEligibility(client)).isFalse();

        InvoiceNumberRange invoiceNumberRange = new InvoiceNumberRange(ubn, invoiceNumberRangeService.getCurrentRangeIdentifier(), "AG", "12340001", "12349999");
        invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);

        assertThat(electronicInvoiceService.checkElectronicInvoiceEligibility(client)).isTrue();

        final ElectronicInvoice electronicInvoice = electronicInvoiceService.createElectronicInvoice(client, order, orderTransaction);

        LOGGER.info("{}", electronicInvoice);

        assertThat(electronicInvoice.getBarcodeContent()).hasSize(19);
        assertThat(StringUtils.substringBefore(electronicInvoice.getQrCode1Content(), ":")).hasSize(77);
        assertThat(electronicInvoice.getQrCode2Content()).startsWith("**");

        assertThat(pendingEInvoiceQueueService.findPendingEInvoicesByUbn(ubn)).hasSize(1);

        electronicInvoiceService.cancelElectronicInvoice(electronicInvoice);

        assertThat(pendingEInvoiceQueueService.findPendingEInvoicesByUbn(ubn)).hasSize(2);
    }

    @Test
    void createElectronicInvoice_NoAvailableInvoiceNumber() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 2);

        final OrderTransaction orderTransaction = new OrderTransaction(order.getId(), client.getId(), order.getOrderTotal(), order.getOrderTotal(), OrderTransaction.PaymentMethod.CARD, OrderTransaction.BillType.SINGLE, List.of());
        orderTransaction.putPaymentDetails(OrderTransaction.PaymentDetailsKey.LAST_FOUR_DIGITS, "1234");

        final ElectronicInvoice electronicInvoice = electronicInvoiceService.createElectronicInvoice(client, order, orderTransaction);
        assertThat(electronicInvoice.getInvoiceNumber()).isEqualTo(ElectronicInvoiceService.INVOICE_NUMBER_MISSING);

        assertThat(pendingEInvoiceQueueService.findPendingEInvoicesByUbn(ubn)).isEmpty();

        InvoiceNumberRange invoiceNumberRange = new InvoiceNumberRange(ubn, invoiceNumberRangeService.getCurrentRangeIdentifier(), "AG", "12340001", "12349999");
        invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);

        electronicInvoiceService.issueNewInvoiceNumber(electronicInvoice);

        assertThat(electronicInvoice.getInvoiceNumber()).isNotEqualTo(ElectronicInvoiceService.INVOICE_NUMBER_MISSING);

        assertThat(pendingEInvoiceQueueService.findPendingEInvoicesByUbn(ubn)).hasSize(1);
    }

    @Test
    void testRounding() {

        BigDecimal amount = new BigDecimal("150");
        BigDecimal tax = new BigDecimal("7.5");

        assertThat(amount.subtract(tax).setScale(0, RoundingMode.UP)).isEqualByComparingTo("143");
    }
}