package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.shared.DummyObjects;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@Transactional
class OrderTransactionServiceImplTest {

    @Autowired
    private OrderTransactionService orderTransactionService;

    @Test
    void createOrderTransaction() {

        final OrderTransaction orderTransaction = new OrderTransaction(new ObjectId().toString(), "dummy-client-id", BigDecimal.valueOf(150), BigDecimal.valueOf(150),
                OrderTransaction.PaymentMethod.CARD,
                OrderTransaction.BillType.SINGLE,
                List.of());

        orderTransactionService.createOrderTransaction(orderTransaction);

        assertThat(orderTransaction.getId()).isNotNull();
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

        final String instruction = orderTransactionService.createOrderDetailsPrintInstruction(client, orderTransaction);

        System.out.println(instruction);
    }
}