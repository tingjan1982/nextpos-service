package io.nextpos.ordermanagement.data;

import lombok.Data;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class InProcessOrderLineItem {

    private String orderId;

    private String serialId;

    private List<Order.TableInfo> tables;

    private String workingArea;

    private String lineItemId;

    private String displayName;

    private String options;

    private int quantity;

    private Date modifiedDate;

    private int order;


    public InProcessOrderLineItem(Order order, String workingArea, OrderLineItem orderLineItem) {

        this.orderId = order.getId();
        this.serialId = order.getSerialId();
        this.tables = order.getTables();
        this.workingArea = workingArea;

        this.lineItemId = orderLineItem.getId();
        this.displayName = orderLineItem.getProductSnapshot().getDisplayName();
        this.options = orderLineItem.getProductOptions();
        this.quantity = orderLineItem.getQuantity();
        this.modifiedDate = orderLineItem.getModifiedDate();
        this.order = orderLineItem.getOrder();
    }

    public static List<InProcessOrderLineItem> orderLineItems(Order order, String workingArea, List<OrderLineItem> filteredLineItems) {

        return filteredLineItems.stream()
                .map(li -> new InProcessOrderLineItem(order, workingArea, li))
                .collect(Collectors.toList());
    }

    public static Comparator<InProcessOrderLineItem> getComparator() {
        return Comparator.comparing(InProcessOrderLineItem::getOrder).thenComparing(InProcessOrderLineItem::getModifiedDate);
    }
}
