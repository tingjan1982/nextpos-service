package io.nextpos.reporting.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTrafficReport {
    
    private List<CustomerTraffic> ordersByHour = new ArrayList<>();

    @Data
    public static class CustomerTraffic {

        private String id;

        private Integer orderCount;

        private Integer customerCount;

        private Integer hourOfDay;

        public static CustomerTraffic emptyObject(String id) {
            final CustomerTraffic emptyCustomerTraffic = new CustomerTraffic();
            emptyCustomerTraffic.setId(id);
            emptyCustomerTraffic.setHourOfDay(Integer.parseInt(id));
            emptyCustomerTraffic.setOrderCount(0);
            emptyCustomerTraffic.setCustomerCount(0);

            return emptyCustomerTraffic;
        }
    }
}
