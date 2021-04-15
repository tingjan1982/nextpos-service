package io.nextpos.ordertransaction.data;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface OrderTransactionRepository extends PagingAndSortingRepository<OrderTransaction, String> {

    List<OrderTransaction> findAllByOrderId(String id);

    boolean existsAllByOrderIdAndStatusNot(String orderId, OrderTransaction.OrderTransactionStatus status);

    boolean existsAllByOrderIdAndInvoiceDetails_ElectronicInvoiceNotNullAndStatusNot(String orderId, OrderTransaction.OrderTransactionStatus status);
}
