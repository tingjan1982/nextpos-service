package io.nextpos.ordertransaction.service;

import io.nextpos.ordertransaction.data.OrderTransaction;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;
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
}