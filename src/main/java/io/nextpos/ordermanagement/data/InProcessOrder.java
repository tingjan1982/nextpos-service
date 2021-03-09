package io.nextpos.ordermanagement.data;

import lombok.Data;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class InProcessOrder {

    private String orderId;

    private String serialId;

    private List<Order.TableInfo> tables;

    private List<InProcessOrderLineItem> orderLineItems;

    private Date createdDate;

    private int order;

    public InProcessOrder(Order order) {
        orderId = order.getId();
        serialId = order.getSerialId();
        tables = order.getTables();
        orderLineItems = order.getOrderLineItems().stream()
                .filter(li -> li.getState().isPreparing())
                .map(li -> new InProcessOrderLineItem(order, li))
                .collect(Collectors.toList());
        this.order = order.getOrder();
        this.createdDate = order.getCreatedDate();
    }

    public static Comparator<InProcessOrder> getComparator() {
        return Comparator.comparing(InProcessOrder::getOrder).thenComparing(Comparator.comparing(InProcessOrder::getCreatedDate).reversed());
    }
}
