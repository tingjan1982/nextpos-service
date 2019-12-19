package io.nextpos.ordertransaction.web;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.ordertransaction.web.model.OrderTransactionRequest;
import io.nextpos.ordertransaction.web.model.OrderTransactionResponse;
import io.nextpos.shared.exception.ClientOwnershipViolationException;
import io.nextpos.shared.web.ClientResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders/transactions")
public class OrderTransactionController {

    private final OrderTransactionService orderTransactionService;

    private final OrderService orderService;

    @Autowired
    public OrderTransactionController(final OrderTransactionService orderTransactionService, final OrderService orderService) {
        this.orderTransactionService = orderTransactionService;
        this.orderService = orderService;
    }

    @PostMapping
    public OrderTransactionResponse createOrderTransaction(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                           @Valid @RequestBody OrderTransactionRequest orderTransactionRequest) {

        OrderTransaction orderTransaction = fromOrderTransactionRequest(client, orderTransactionRequest);
        final OrderTransaction createdOrderTransaction = orderTransactionService.createOrderTransaction(orderTransaction);
        final String orderDetailsPrintInstruction = orderTransactionService.createOrderDetailsPrintInstruction(client, orderTransaction);

        return toOrderTransactionResponse(client, createdOrderTransaction, orderDetailsPrintInstruction);
    }

    @GetMapping("/{id}")
    public OrderTransactionResponse getOrderTransaction(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @PathVariable String id) {

        final OrderTransaction orderTransaction = orderTransactionService.getOrderTransaction(id);

        return toOrderTransactionResponse(client, orderTransaction, null);
    }

    private OrderTransaction fromOrderTransactionRequest(final Client client, final OrderTransactionRequest orderTransactionRequest) {

        final Order order = orderService.getOrder(orderTransactionRequest.getOrderId());

        if (!StringUtils.equals(order.getClientId(), client.getId())) {
            final String errorMsg = String.format("Order client id and authenticated client id does not match, order cid=%s, auth cid=%s",
                    order.getClientId(), client.getId());

            throw new ClientOwnershipViolationException(errorMsg);
        }


        final List<OrderTransaction.BillLineItem> billLineItems = populateBillLineItems(order, orderTransactionRequest);
        final BigDecimal settleAmount = billLineItems.stream()
                .map(OrderTransaction.BillLineItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderTransaction(order.getId(),
                order.getClientId(),
                order.getOrderTotal(),
                settleAmount,
                OrderTransaction.PaymentMethod.valueOf(orderTransactionRequest.getPaymentMethod()),
                OrderTransaction.BillType.valueOf(orderTransactionRequest.getBillType()),
                billLineItems);
    }

    private List<OrderTransaction.BillLineItem> populateBillLineItems(final Order order, final OrderTransactionRequest orderTransactionRequest) {

        final OrderTransaction.BillType billType = OrderTransaction.BillType.valueOf(orderTransactionRequest.getBillType());

        switch(billType) {
            case SINGLE:
                return List.of(new OrderTransaction.BillLineItem("single", 1, order.getOrderTotal()));

            case CUSTOM:
                return orderTransactionRequest.getBillLineItems().stream().map(liRequest -> {
                    final OrderLineItem orderLineItem = order.getOrderLineItem(liRequest.getLineItemId());

                    final BigDecimal subTotal = orderLineItem.getProductSnapshot().getPrice().multiply(BigDecimal.valueOf(liRequest.getQuantity()));
                    return new OrderTransaction.BillLineItem(orderLineItem.getProductSnapshot().getName(), liRequest.getQuantity(), subTotal);
                }).collect(Collectors.toList());

            case SPLIT:
                // todo: figure out rounding problem.
                final BigDecimal dividend = order.getTotal().getAmountWithTax();
                final BigDecimal divisor = BigDecimal.valueOf(orderTransactionRequest.getSplitWith());
                final BigDecimal splitSubTotal = dividend.divide(divisor, RoundingMode.DOWN);

                // todo: figure i18n
                return List.of(new OrderTransaction.BillLineItem("split", 1, splitSubTotal));

            default:
                return List.of();
        }
    }

    private OrderTransactionResponse toOrderTransactionResponse(final Client client, final OrderTransaction orderTransaction, final String orderDetailsPrintInstruction) {

        final List<OrderTransactionResponse.BillLineItemResponse> billLineItems = orderTransaction.getBillDetails().getBillLineItems().stream()
                .map(li -> new OrderTransactionResponse.BillLineItemResponse(li.getName(), li.getQuantity(), li.getSubTotal()))
                .collect(Collectors.toList());

        return new OrderTransactionResponse(orderTransaction.getId(),
                orderTransaction.getOrderId(),
                client.getClientName(),
                client.getAttribute(Client.ClientAttributes.UBN.name()),
                orderTransaction.getOrderTotal(),
                orderTransaction.getSettleAmount(),
                orderTransaction.getBillDetails().getBillType(),
                billLineItems,
                orderDetailsPrintInstruction);
    }
}
