package io.nextpos.ordertransaction.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.ordertransaction.web.model.OrderTransactionRequest;
import io.nextpos.ordertransaction.web.model.OrderTransactionResponse;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.workingarea.service.PrinterInstructionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders/transactions")
public class OrderTransactionController {

    private final OrderTransactionService orderTransactionService;

    private final OrderService orderService;

    private final PrinterInstructionService printerInstructionService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    private final ConversionService conversionService;

    @Autowired
    public OrderTransactionController(final OrderTransactionService orderTransactionService, final OrderService orderService, final PrinterInstructionService printerInstructionService, final ClientObjectOwnershipService clientObjectOwnershipService, final ConversionService conversionService) {
        this.orderTransactionService = orderTransactionService;
        this.orderService = orderService;
        this.printerInstructionService = printerInstructionService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
        this.conversionService = conversionService;
    }

    @PostMapping
    public OrderTransactionResponse createOrderTransaction(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                           @Valid @RequestBody OrderTransactionRequest orderTransactionRequest) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(orderTransactionRequest.getOrderId()));
        OrderTransaction orderTransaction = fromOrderTransactionRequest(order, orderTransactionRequest);
        final OrderTransaction createdOrderTransaction = orderTransactionService.createOrderTransaction(client, orderTransaction);

        return OrderTransactionResponse.toOrderTransactionResponse(createdOrderTransaction);
    }

    @GetMapping("/{transactionId}")
    public OrderTransactionResponse getOrderTransaction(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                        @PathVariable String transactionId) {

        final OrderTransaction orderTransaction = orderTransactionService.getOrderTransaction(transactionId);
        final Order order = orderService.getOrder(orderTransaction.getOrderId());

        final String receiptXML = printerInstructionService.createOrderDetailsPrintInstruction(client, order, orderTransaction);
        final String electronicInvoiceXML = printerInstructionService.createElectronicInvoiceXML(client, order, orderTransaction);

        final OrderTransactionResponse response = OrderTransactionResponse.toOrderTransactionResponse(orderTransaction);
        response.setReceiptXML(receiptXML);
        response.setInvoiceXML(electronicInvoiceXML);

        return response;
    }

    @PostMapping("{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelOrderTransaction(@PathVariable String id) {

        orderTransactionService.cancelOrderTransaction(id);
    }

    @PostMapping("{id}/void")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void voidOrderTransaction(@PathVariable String id) {

        orderTransactionService.voidOrderTransaction(id);
    }

    private OrderTransaction fromOrderTransactionRequest(final Order order, final OrderTransactionRequest orderTransactionRequest) {

        final List<OrderTransaction.BillLineItem> billLineItems = populateBillLineItems(order, orderTransactionRequest);
        final BigDecimal settleAmount = billLineItems.stream()
                .map(OrderTransaction.BillLineItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        final OrderTransaction orderTransaction = new OrderTransaction(order.getId(),
                order.getClientId(),
                order.getOrderTotal(),
                settleAmount,
                OrderTransaction.PaymentMethod.valueOf(orderTransactionRequest.getPaymentMethod()),
                OrderTransaction.BillType.valueOf(orderTransactionRequest.getBillType()),
                billLineItems);

        orderTransaction.updateInvoiceDetails(orderTransactionRequest.getTaxIdNumber(),
                orderTransactionRequest.getCarrierType(),
                orderTransactionRequest.getCarrierId(),
                orderTransactionRequest.getNpoBan(),
                orderTransactionRequest.isPrintMark());

        orderTransactionRequest.getPaymentDetails().forEach((key, value) -> {
            final Class<?> targetValueType = key.getValueType();
            final Object convertedValue = conversionService.convert(value, targetValueType);
            orderTransaction.putPaymentDetails(key, convertedValue);
        });

        validateCashChange(orderTransaction);

        return orderTransaction;
    }

    private void validateCashChange(OrderTransaction orderTransaction) {

        if (orderTransaction.getPaymentMethod() == OrderTransaction.PaymentMethod.CASH) {
            final BigDecimal cash = orderTransaction.getPaymentDetailsByKey(OrderTransaction.PaymentDetailsKey.CASH);

            if (cash != null) {
                final BigDecimal settleAmount = orderTransaction.getSettleAmount();
                if (cash.compareTo(settleAmount) < 0) {
                    throw new BusinessLogicException("message.insufficientCashAmount", "Entered cash amount is less than the settling amount: " + settleAmount);
                }

                final BigDecimal cashChange = cash.subtract(settleAmount);
                orderTransaction.putPaymentDetails(OrderTransaction.PaymentDetailsKey.CASH_CHANGE, cashChange);
            }
        }
    }

    private List<OrderTransaction.BillLineItem> populateBillLineItems(final Order order, final OrderTransactionRequest orderTransactionRequest) {

        final OrderTransaction.BillType billType = OrderTransaction.BillType.valueOf(orderTransactionRequest.getBillType());

        switch (billType) {
            case SINGLE:
                final List<OrderTransaction.BillLineItem> billLIneItems = order.getOrderLineItems().stream()
                        .map(li -> new OrderTransaction.BillLineItem(li.getProductSnapshot().getName(),
                                li.getQuantity(),
                                li.getProductPriceWithOptions().getAmountWithTax(),
                                li.getDeducedSubTotal().getAmountWithTax()))
                        .collect(Collectors.toList());

                if (order.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    final BigDecimal discount = order.deduceRoundingAmount(() -> order.getDiscount().negate());
                    billLIneItems.add(new OrderTransaction.BillLineItem("discount", 1, discount, discount));
                }

                if (order.getServiceCharge().compareTo(BigDecimal.ZERO) > 0) {
                    final BigDecimal serviceCharge = order.deduceRoundingAmount(order::getServiceCharge);
                    billLIneItems.add(new OrderTransaction.BillLineItem("serviceCharge", 1, serviceCharge, serviceCharge));
                }

                return billLIneItems;

            case SPLIT:
                return List.of(new OrderTransaction.BillLineItem("split", 1, orderTransactionRequest.getSettleAmount(), orderTransactionRequest.getSettleAmount()));
            default:
                return List.of();
        }
    }
}
