package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class OrdersResponse {

    /**
     * Key is table layout id, value is list of LightOrderResponse.
     */
    private Map<String, List<LightOrderResponse>> orders;

    public OrdersResponse(List<Order> orders) {

        this.orders = orders.stream()
                .map(o -> {
                    final boolean itemNeedAttention = o.getOrderLineItems().stream()
                            .anyMatch(li -> DateUtils.addMinutes(li.getCreatedDate(), 30).before(new Date()));

                    return new OrdersResponse.LightOrderResponse(o.getId(),
                            o.getSerialId(),
                            StringUtils.substringAfter(o.getSerialId(), "-"),
                            o.getOrderType(),
                            o.getOneTableInfo().getTableLayoutId(),
                            o.getOneTableInfo().getTableLayoutName(),
                            o.getOneTableInfo().getTableId(),
                            o.getOneTableInfo().getDisplayName(),
                            o.getTables(),
                            o.getDemographicData().getCustomerCount(),
                            o.getCreatedDate(),
                            o.getState(),
                            o.getOrderTotal(),
                            itemNeedAttention);
                })
                .collect(Collectors.groupingBy(OrdersResponse.LightOrderResponse::getTableLayoutId, Collectors.toList()));
    }

    @Data
    @AllArgsConstructor
    public static class LightOrderResponse {

        private String orderId;

        private String serialId;

        private String serialIdSuffix;

        private Order.OrderType orderType;

        private String tableLayoutId;

        private String tableLayout;

        @Deprecated
        private String tableId;

        @Deprecated
        private String tableName;

        private List<Order.TableInfo> tables;

        private int customerCount;

        private Date createdTime;

        private Order.OrderState state;

        private BigDecimal orderTotal;

        private boolean itemNeedAttention;
    }
}
