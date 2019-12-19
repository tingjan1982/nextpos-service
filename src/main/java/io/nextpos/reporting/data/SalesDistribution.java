package io.nextpos.reporting.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesDistribution {

    private List<WeeklySales> salesByWeek;

    private List<MonthlySales> salesByMonth;

    @Data
    public static class WeeklySales {

        private String id;

        private String week;

        private BigDecimal total;
    }

    @Data
    public static class MonthlySales {

        private String id;

        private String month;

        private BigDecimal total;
    }
}
