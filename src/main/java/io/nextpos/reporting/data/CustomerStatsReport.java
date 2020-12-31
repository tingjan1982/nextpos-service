package io.nextpos.reporting.data;

import io.nextpos.datetime.data.ZonedDateRange;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatsReport {

    private ZonedDateRange dateRange;

    private List<CustomerStats> groupedCustomerStats = new ArrayList<>();

    @Data
    public static class CustomerStats {

        private String id;

        private BigDecimal total = BigDecimal.ZERO;

        private BigDecimal averageSpending = BigDecimal.ZERO;

        private int customerCount;
    }
}
