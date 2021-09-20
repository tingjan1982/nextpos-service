package io.nextpos.ordermanagement.web.factory;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.web.model.ComboOrderLineItemRequest;
import io.nextpos.ordermanagement.web.model.OrderLineItemRequest;
import io.nextpos.ordermanagement.web.model.OrderRequest;

public interface OrderCreationFactory {

    Order newOrder(final Client client, OrderRequest orderRequest);

    void updateTableInfoAndDemographicData(Order order, OrderRequest orderRequest);

    OrderLineItem newOrderLineItem(Client client, ComboOrderLineItemRequest li);

    OrderLineItem newOrderLineItem(Client client, OrderLineItemRequest li);
}
