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
public class SalesDistribution {

    private List<WeeklySales> salesByWeek = new ArrayList<>();

    private List<MonthlySales> salesByMonth = new ArrayList<>();

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

        private LocalDate date;

        private BigDecimal total = BigDecimal.ZERO;
    }
}
