package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordertransaction.data.ElectronicInvoice;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class ElectronicInvoiceServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElectronicInvoiceServiceImplTest.class);

    @Autowired
    private ElectronicInvoiceService electronicInvoiceService;

    @Autowired
    private OrderSettings orderSettings;

    @Test
    void createElectronicInvoice() {

        final Client client = DummyObjects.dummyClient();
        client.addAttribute(Client.ClientAttributes.UBN.name(), "27252210");
        client.addAttribute(Client.ClientAttributes.AES_KEY.name(), "12341234123412341234123412341234");
        final Order order = new Order("client", orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 2);

        final OrderTransaction orderTransaction = new OrderTransaction(order.getId(), "clietnt", order.getOrderTotal(), order.getOrderTotal(), OrderTransaction.PaymentMethod.CARD, OrderTransaction.BillType.SINGLE, List.of());
        orderTransaction.setTaxIdNumber("27252210");
        orderTransaction.putPaymentDetails(OrderTransaction.PaymentDetailsKey.LAST_FOUR_DIGITS, "1234");

        final ElectronicInvoice electronicInvoice = electronicInvoiceService.createElectronicInvoice(client, order, orderTransaction);

        LOGGER.info("{}", electronicInvoice);
    }
}