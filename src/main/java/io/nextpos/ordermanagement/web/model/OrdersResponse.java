package io.nextpos.ordermanagement.web.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.TaxableAmount;
import io.nextpos.reporting.data.ReportDateParameter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class OrdersResponse {

    /**
     * inflight orders don't care about report parameter so omitted for null scenarios.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ReportDateParameter reportParameter;

    /**
     * Key is table layout id, value is list of LightOrderResponse.
     */
    private Map<String, List<LightOrderResponse>> orders;

    @Data
    @AllArgsConstructor
    public static class LightOrderResponse {

        private String orderId;

        private String tableLayoutId;

        private String tableLayout;

        private String tableName;

        private int customerCount;

        private Date createdTime;

        private Order.OrderState state;

        private TaxableAmount total;
    }
}
