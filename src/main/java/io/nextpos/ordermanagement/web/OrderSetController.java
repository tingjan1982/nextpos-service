package io.nextpos.ordermanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.ordermanagement.data.OrderSet;
import io.nextpos.ordermanagement.service.OrderSetService;
import io.nextpos.ordermanagement.web.model.OrderSetRequest;
import io.nextpos.ordermanagement.web.model.OrderSetResponse;
import io.nextpos.ordermanagement.web.model.OrderSetsResponse;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ordersets")
public class OrderSetController {

    private final OrderSetService orderSetService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    @Autowired
    public OrderSetController(OrderSetService orderSetService, ClientObjectOwnershipService clientObjectOwnershipService) {
        this.orderSetService = orderSetService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
    }

    @PostMapping
    public OrderSetResponse createOrderSet(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                           @Valid @RequestBody OrderSetRequest orderSetRequest) {

        final OrderSet orderSet = orderSetService.createOrderSet(client.getId(), orderSetRequest.getOrderIds());

        return toOrderSetResponse(orderSet);
    }

    @GetMapping
    public OrderSetsResponse getOrderSets(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<OrderSetResponse> orderSetResponses = orderSetService.getInFlightOrderSets(client.getId()).stream()
                .map(this::toOrderSetResponse).collect(Collectors.toList());

        return new OrderSetsResponse(orderSetResponses);
    }

    @GetMapping("/{id}")
    public OrderSetResponse getOrderSet(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                        @PathVariable String id) {

        final OrderSet orderSet = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderSetService.getOrderSet(id));

        return toOrderSetResponse(orderSet);
    }

    @PostMapping("/{id}/merge")
    public OrderSetResponse mergeOrderSet(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                          @PathVariable String id) {

        final OrderSet orderSet = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderSetService.getOrderSet(id));
        orderSetService.mergeOrderSet(orderSet);

        return toOrderSetResponse(orderSet);
    }

    @PostMapping("/{id}/complete")
    public OrderSetResponse completeOrderSet(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                             @PathVariable String id) {

        final OrderSet orderSet = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderSetService.getOrderSet(id));
        return toOrderSetResponse(orderSetService.completeOrderSet(orderSet));
    }

    private OrderSetResponse toOrderSetResponse(OrderSet orderSet) {

        return new OrderSetResponse(orderSet.getId(),
                orderSet.getMainOrderId(),
                orderSet.getLinkedOrders(),
                orderSet.getTableLayoutId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrderSet(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                               @PathVariable String id) {

        final OrderSet orderSet = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderSetService.getOrderSet(id));
        orderSetService.deleteOrderSet(orderSet);
    }
}
