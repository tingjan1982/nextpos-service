package io.nextpos.ordertransaction.service;

import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.data.OrderTransactionRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class OrderTransactionServiceImpl implements OrderTransactionService {

    private final OrderTransactionRepository orderTransactionRepository;

    @Autowired
    public OrderTransactionServiceImpl(final OrderTransactionRepository orderTransactionRepository) {
        this.orderTransactionRepository = orderTransactionRepository;
    }

    @Override
    public OrderTransaction createOrderTransaction(final OrderTransaction orderTransaction) {

        orderTransaction.getPaymentMethodDetails().setPaymentStatus(OrderTransaction.PaymentStatus.SUCCESS);
        final String invoiceNumber = this.getInvoiceNumberExternally();
        orderTransaction.getInvoiceDetails().setInvoiceNumber(invoiceNumber);

        return orderTransactionRepository.save(orderTransaction);
    }

    @Override
    public OrderTransaction getOrderTransaction(final String id) {

        return orderTransactionRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, OrderTransaction.class);
        });
    }

    private String getInvoiceNumberExternally() {
        return "DUMMY-E-INVOICE-NUMBER";
    }
}
