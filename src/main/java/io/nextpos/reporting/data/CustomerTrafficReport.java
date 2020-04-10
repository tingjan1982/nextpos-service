package io.nextpos.reporting.data;

import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTrafficReport {

    private List<TotalCount> counts = new ArrayList<>();

    private List<CustomerTraffic> ordersByHour = new ArrayList<>();

    private List<OrdersByType> ordersByType = new ArrayList<>();

    private List<OrdersByAgeGroup> ordersByAgeGroup = new ArrayList<>();

    private List<OrdersByVisitFrequency> ordersByVisitFrequency = new ArrayList<>();


    public Optional<TotalCount> getTotalCountObject() {
        return CollectionUtils.isEmpty(counts) ? Optional.empty() : Optional.of(counts.get(0));
    }

    public void enhanceResults() {

        getTotalCountObject().ifPresent(total -> {
            ReportEnhancer.enhanceReportResult(IntStream.rangeClosed(0, 23),
                    () -> this.getOrdersByHour().stream().collect(Collectors.toMap(CustomerTrafficReport.CustomerTraffic::getId, s -> s)),
                    CustomerTrafficReport.CustomerTraffic::emptyObject,
                    this::setOrdersByHour);

            ReportEnhancer.enhanceReportResult(Order.OrderType.values(),
                    () -> this.getOrdersByType().stream().collect(Collectors.toMap(CustomerTrafficReport.OrdersByType::getId, s -> s)),
                    CustomerTrafficReport.OrdersByType::emptyObject,
                    this::setOrdersByType);

            ReportEnhancer.enhanceReportResult(Order.DemographicData.AgeGroup.values(),
                    () -> this.getOrdersByAgeGroup().stream().collect(Collectors.toMap(CustomerTrafficReport.OrdersByAgeGroup::getId, s -> s)),
                    CustomerTrafficReport.OrdersByAgeGroup::emptyObject,
                    this::setOrdersByAgeGroup);

            ReportEnhancer.enhanceReportResult(Order.DemographicData.VisitFrequency.values(),
                    () -> this.getOrdersByVisitFrequency().stream().collect(Collectors.toMap(CustomerTrafficReport.OrdersByVisitFrequency::getId, s -> s)),
                    CustomerTrafficReport.OrdersByVisitFrequency::emptyObject,
                    this::setOrdersByVisitFrequency);

        });
    }

    @Data
    public static class CustomerTraffic {

        private String id;

        private int orderCount;

        private int customerCount;

        private int hourOfDay;

        public static CustomerTraffic emptyObject(String id) {
            final CustomerTraffic emptyCustomerTraffic = new CustomerTraffic();
            emptyCustomerTraffic.setId(id);
            emptyCustomerTraffic.setHourOfDay(Integer.parseInt(id));
            emptyCustomerTraffic.setOrderCount(0);
            emptyCustomerTraffic.setCustomerCount(0);

            return emptyCustomerTraffic;
        }
    }

    @Data
    public static class OrdersByType {

        private Order.OrderType id;

        private Order.OrderType orderType;

        private int orderCount;

        public static OrdersByType emptyObject(Order.OrderType id) {

            final OrdersByType emptyOrdersByType = new OrdersByType();
            emptyOrdersByType.setId(id);
            emptyOrdersByType.setOrderType(id);

            return emptyOrdersByType;
        }
    }

    @Data
    public static class OrdersByAgeGroup {

        private Order.DemographicData.AgeGroup id;

        private Order.DemographicData.AgeGroup ageGroup;

        private int orderCount;

        public static OrdersByAgeGroup emptyObject(Order.DemographicData.AgeGroup id) {

            final OrdersByAgeGroup emptyOrdersByAgeGroup = new OrdersByAgeGroup();
            emptyOrdersByAgeGroup.setId(id);
            emptyOrdersByAgeGroup.setAgeGroup(id);

            return emptyOrdersByAgeGroup;
        }
    }

    @Data
    public static class OrdersByVisitFrequency {

        private Order.DemographicData.VisitFrequency id;

        private Order.DemographicData.VisitFrequency visitFrequency;

        private int orderCount;

        public static OrdersByVisitFrequency emptyObject(final Order.DemographicData.VisitFrequency id) {

            final OrdersByVisitFrequency emptyOrdersByVisitFrequency = new OrdersByVisitFrequency();
            emptyOrdersByVisitFrequency.setId(id);
            emptyOrdersByVisitFrequency.setVisitFrequency(id);

            return emptyOrdersByVisitFrequency;
        }
    }

    @Data
    public static class TotalCount {

        private String id;

        private int orderCount;

        private int maleCount;

        private int femaleCount;

        private int kidCount;

        private int customerCount;

    }
}
