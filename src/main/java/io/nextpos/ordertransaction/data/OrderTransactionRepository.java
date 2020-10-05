package io.nextpos.ordertransaction.data;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

public interface OrderTransactionRepository extends PagingAndSortingRepository<OrderTransaction, String> {

    List<OrderTransaction> findAllByOrderId(String id);

    boolean existsAllByOrderIdAndStatusNot(String orderId, OrderTransaction.OrderTransactionStatus status);

    Optional<OrderTransaction> findByInvoiceDetails_ElectronicInvoice_InvoiceNumber(String invoiceNumber);
}
