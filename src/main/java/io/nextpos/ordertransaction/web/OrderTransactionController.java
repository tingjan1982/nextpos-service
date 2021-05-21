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
                                                        @PathVariable String transactionId,
                                                        @RequestParam(value = "reprint", required = false) boolean reprint) {

        final OrderTransaction orderTransaction = orderTransactionService.getOrderTransaction(transactionId);
        final Order order = orderService.getOrder(orderTransaction.getOrderId());

        final OrderTransactionResponse response = OrderTransactionResponse.toOrderTransactionResponse(orderTransaction);
        final String receiptXML = printerInstructionService.createOrderDetailsPrintInstruction(client, order, orderTransaction);
        response.setReceiptXML(receiptXML);

        if (orderTransaction.hasPrintableElectronicInvoice()) {
            final String electronicInvoiceXML = printerInstructionService.createElectronicInvoiceXML(client, order, orderTransaction, reprint);
            response.setInvoiceXML(electronicInvoiceXML);
        }

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

         final OrderTransaction orderTransaction = new OrderTransaction(order,
                OrderTransaction.PaymentMethod.valueOf(orderTransactionRequest.getPaymentMethod()),
                OrderTransaction.BillType.valueOf(orderTransactionRequest.getBillType()),
                orderTransactionRequest.getSettleAmount());

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

        if (OrderTransaction.PaymentMethod.CASH.name().equals(orderTransaction.getPaymentMethod())) {
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
}
