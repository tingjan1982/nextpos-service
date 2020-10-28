package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.data.OrderTransactionRepository;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(transactionManager = "chainedTransactionManager")
public class OrderTransactionServiceImpl implements OrderTransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderTransactionServiceImpl.class);

    private final ElectronicInvoiceService electronicInvoiceService;

    private final OrderTransactionRepository orderTransactionRepository;

    private final OrderService orderService;

    @Autowired
    public OrderTransactionServiceImpl(final ElectronicInvoiceService electronicInvoiceService, final OrderTransactionRepository orderTransactionRepository, final OrderService orderService) {
        this.electronicInvoiceService = electronicInvoiceService;
        this.orderTransactionRepository = orderTransactionRepository;
        this.orderService = orderService;
    }

    @Override
    public OrderTransaction createOrderTransaction(final Client client, final OrderTransaction orderTransaction) {

        final Order order = orderService.getOrder(orderTransaction.getOrderId());

        if (Order.OrderState.finalStates().contains(order.getState())) {
            throw new BusinessLogicException("Cannot create order transaction. The order has been finalized: " + order.getId());
        }

        LOGGER.info("Creating order transaction for order {}", order.getId());

        // todo: this would change when we connect to external credit card processor
        orderTransaction.getPaymentDetails().setPaymentStatus(OrderTransaction.PaymentStatus.SUCCESS);

        this.createAndSaveElectronicInvoice(client, order, orderTransaction);

        final OrderTransaction updatedTransaction = orderTransactionRepository.save(orderTransaction);

        final BigDecimal settledAmount = orderTransactionRepository.findAllByOrderId(orderTransaction.getOrderId()).stream()
                .map(OrderTransaction::getSettleAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (order.getOrderTotal().compareTo(settledAmount) <= 0) {
            LOGGER.info("Order[{}] with amount {} has been settled, settling the order.", order.getId(), order.getOrderTotal());
            orderService.performOrderAction(orderTransaction.getOrderId(), Order.OrderAction.SETTLE);
        }

        return updatedTransaction;
    }

    private void createAndSaveElectronicInvoice(final Client client, final Order order, final OrderTransaction orderTransaction) {

        if (!electronicInvoiceService.checkElectronicInvoiceEligibility(client)) {
            LOGGER.info("Client {} does not have electronic invoice setup.", client.getId());
            return;
        }

        final ElectronicInvoice electronicInvoice = electronicInvoiceService.createElectronicInvoice(client, order, orderTransaction);

        final OrderTransaction.InvoiceDetails invoiceDetails = orderTransaction.getInvoiceDetails();
        invoiceDetails.setElectronicInvoice(electronicInvoice);
    }

    @Override
    public OrderTransaction getOrderTransaction(final String id) {

        return orderTransactionRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, OrderTransaction.class);
        });
    }

    @Override
    public Order getOrderByInvoiceNumber(String internalInvoiceNumber) {

        final ElectronicInvoice electronicInvoice = electronicInvoiceService.getElectronicInvoiceByInvoiceNumber(internalInvoiceNumber);
        return orderService.getOrder(electronicInvoice.getOrderId());
    }

    @Override
    public List<OrderTransaction> getOrderTransactionByOrderId(String orderId) {
        return orderTransactionRepository.findAllByOrderId(orderId);
    }

    @Override
    public void cancelOrderTransaction(String id) {

        final OrderTransaction orderTransaction = this.getOrderTransaction(id);

        if (orderTransaction.getInvoiceDetails().getElectronicInvoice() == null) {
            throw new ObjectNotFoundException(id, ElectronicInvoice.class);
        }

        electronicInvoiceService.cancelElectronicInvoice(orderTransaction.getInvoiceDetails().getElectronicInvoice());

        orderTransaction.setStatus(OrderTransaction.OrderTransactionStatus.CANCELLED);
        orderTransactionRepository.save(orderTransaction);

        final boolean hasNonCancelledTransactions = orderTransactionRepository.existsAllByOrderIdAndStatusNot(orderTransaction.getOrderId(), OrderTransaction.OrderTransactionStatus.CANCELLED);

        if (!hasNonCancelledTransactions) {
            LOGGER.info("All transactions are cancelled, mark associated order as deleted: {}", orderTransaction.getOrderId());
            orderService.performOrderAction(orderTransaction.getOrderId(), Order.OrderAction.CANCEL);
        }
    }
}
