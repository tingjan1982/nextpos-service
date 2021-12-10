package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderCriteria;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.data.OrderTransactionRepository;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@ChainedTransaction
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

        if (order.isClosed()) {
            throw new BusinessLogicException("message.orderFinalized", "Cannot create order transaction. The order has been finalized: " + order.getId());
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

        if (order.getOrderTotal().compareTo(BigDecimal.ZERO) > 0) {
            final ElectronicInvoice electronicInvoice = electronicInvoiceService.createElectronicInvoice(client, order, orderTransaction);

            final OrderTransaction.InvoiceDetails invoiceDetails = orderTransaction.getInvoiceDetails();
            invoiceDetails.setElectronicInvoice(electronicInvoice);
        }
    }

    @Override
    public void saveOrderTransaction(OrderTransaction orderTransaction) {
        orderTransactionRepository.save(orderTransaction);
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
    public List<Order> getCancellableOrders(Client client, ZonedDateRange zonedDateRange) {

        return orderService.getOrders(client, zonedDateRange, OrderCriteria.instance().orderState(Order.OrderState.DELETED)).stream()
                .filter(o -> orderTransactionRepository.existsAllByOrderIdAndInvoiceDetails_ElectronicInvoiceNotNullAndStatusNot(o.getId(), OrderTransaction.OrderTransactionStatus.CANCELLED))
                .collect(Collectors.toList());
    }

    @Override
    public void cancelOrderTransaction(String id) {

        final OrderTransaction orderTransaction = this.getOrderTransaction(id);
        final Optional<ElectronicInvoice> electronicInvoice = orderTransaction.getElectronicInvoice();

        if (electronicInvoice.isEmpty()) {
            throw new ObjectNotFoundException(id, ElectronicInvoice.class);
        }

        electronicInvoiceService.cancelElectronicInvoice(electronicInvoice.get());

        orderTransaction.setStatus(OrderTransaction.OrderTransactionStatus.CANCELLED);
        orderTransactionRepository.save(orderTransaction);

        final boolean hasNonCancelledTransactions = orderTransactionRepository.existsAllByOrderIdAndStatusNot(orderTransaction.getOrderId(), OrderTransaction.OrderTransactionStatus.CANCELLED);

        if (!hasNonCancelledTransactions) {
            LOGGER.info("All transactions are cancelled, mark associated order as cancelled: {}", orderTransaction.getOrderId());
            orderService.performOrderAction(orderTransaction.getOrderId(), Order.OrderAction.CANCEL);
        }
    }

    @Override
    public void voidOrderTransaction(String id) {

        final OrderTransaction orderTransaction = this.getOrderTransaction(id);
        final Optional<ElectronicInvoice> electronicInvoice = orderTransaction.getElectronicInvoice();

        if (electronicInvoice.isEmpty()) {
            throw new ObjectNotFoundException(id, ElectronicInvoice.class);
        }

        electronicInvoiceService.voidElectronicInvoice(electronicInvoice.get());

        orderTransaction.setStatus(OrderTransaction.OrderTransactionStatus.VOIDED);
        orderTransactionRepository.save(orderTransaction);

        final boolean hasNonVoidedTransactions = orderTransactionRepository.existsAllByOrderIdAndStatusNot(orderTransaction.getOrderId(), OrderTransaction.OrderTransactionStatus.VOIDED);

        if (!hasNonVoidedTransactions) {
            LOGGER.info("All transactions are voided, mark associated order as voided: {}", orderTransaction.getOrderId());
            orderService.performOrderAction(orderTransaction.getOrderId(), Order.OrderAction.VOID);
        }
    }
}
