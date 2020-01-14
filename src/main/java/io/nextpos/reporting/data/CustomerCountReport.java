package io.nextpos.reporting.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCountReport {

    private List<CustomerCount> groupedCustomerCount;

    @Data
    public static class CustomerCount {

        private String id;

        private LocalDate date;

        private int customerCount;

        private int maleCount;

        private int femaleCount;

        private int kidCount;
    }
}
