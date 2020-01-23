package io.nextpos.reporting.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatsReport {

    private List<CustomerStats> groupedCustomerStats = new ArrayList<>();

    @Data
    public static class CustomerStats {

        private String id;

        private LocalDate date;

        private BigDecimal averageSpending = BigDecimal.ZERO;

        private BigDecimal total = BigDecimal.ZERO;

        private int customerCount;

        private int maleCount;

        private int femaleCount;

        private int kidCount;
    }
}
