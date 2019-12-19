package io.nextpos.reporting.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SalesProgress {

    private List<DailySales> dailySales;

    private List<WeeklySales> weeklySales;

    private List<MonthlySales> monthlySales;


    public BigDecimal getDailySalesProgress() {

        final DailySales sales = dailySales.stream()
                .filter(s -> !s.getId().equals("Other"))
                .findFirst().orElse(new DailySales("0", "0", BigDecimal.ZERO));

        return sales.getTotal();
    }

    public BigDecimal getWeeklySalesProgress() {

        final WeeklySales sales = weeklySales.stream()
                .filter(s -> !s.getId().equals("Other"))
                .findFirst().orElse(new WeeklySales("0", "0", BigDecimal.ZERO));

        return sales.getTotal();
    }

    public BigDecimal getMonthlySalesProgress() {

        final MonthlySales sales = monthlySales.stream()
                .filter(s -> !s.getId().equals("Other"))
                .findFirst().orElse(new MonthlySales("0", "0", BigDecimal.ZERO));

        return sales.getTotal();
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySales {

        private String id;

        private String dayOfMonth;

        private BigDecimal total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklySales {

        private String id;

        private String week;

        private BigDecimal total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySales {

        private String id;

        private String month;

        private BigDecimal total;
    }
}
