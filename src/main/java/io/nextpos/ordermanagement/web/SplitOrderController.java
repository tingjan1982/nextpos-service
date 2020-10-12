package io.nextpos.ordermanagement.web;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.SplitAmountDetails;
import io.nextpos.ordermanagement.service.SplitOrderService;
import io.nextpos.ordermanagement.web.model.OrderResponse;
import io.nextpos.ordermanagement.web.model.SplitOrderByHeadCountRequest;
import io.nextpos.ordermanagement.web.model.SplitOrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/splitOrders")
public class SplitOrderController {

    private final SplitOrderService splitOrderService;

    @Autowired
    public SplitOrderController(SplitOrderService splitOrderService) {
        this.splitOrderService = splitOrderService;
    }

    @PostMapping
    public OrderResponse createSplitOrder(@Valid @RequestBody SplitOrderRequest splitOrderRequest) {

        final Order targetOrder = splitOrderService.newSplitOrder(splitOrderRequest.getSourceOrderId(), splitOrderRequest.getSourceLineItemId());

        return OrderResponse.toOrderResponse(targetOrder);
    }

    @PostMapping("/{targetOrderId}")
    public OrderResponse updateSplitOrder(@PathVariable String targetOrderId,
                                          @Valid @RequestBody SplitOrderRequest request) {

        final Order targetOrder = splitOrderService.updateLineItem(request.getSourceOrderId(), targetOrderId, request.getSourceLineItemId());

        return OrderResponse.toOrderResponse(targetOrder);
    }

    @PostMapping("/{targetOrderId}/revert")
    public OrderResponse revertSplitOrder(@PathVariable String targetOrderId,
                                          @RequestParam("sourceOrderId") String sourceOrderId) {

        final Order sourceOrder = splitOrderService.revertSplitOrderLineItems(targetOrderId, sourceOrderId);

        return OrderResponse.toOrderResponse(sourceOrder);
    }

    @PostMapping("/headcount/{sourceOrderId}")
    public List<SplitAmountDetails> splitByHeadCount(@PathVariable String sourceOrderId,
                                                     @Valid @RequestBody SplitOrderByHeadCountRequest request) {

        return splitOrderService.splitByHeadCount(sourceOrderId, request.getHeadCount());
    }
}
