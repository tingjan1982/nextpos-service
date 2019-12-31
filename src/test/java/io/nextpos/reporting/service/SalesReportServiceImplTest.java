package io.nextpos.reporting.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.reporting.data.RangedSalesReport;
import io.nextpos.reporting.data.SalesDistribution;
import io.nextpos.reporting.data.SalesProgress;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@SpringBootTest
@Transactional
class SalesReportServiceImplTest {

    @Autowired
    private SalesReportService salesReportService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderSettings orderSettings;

    @Autowired
    private MongoTemplate mongoTemplate;


    @AfterEach
    void removeData() {
        mongoTemplate.findAllAndRemove(new Query(), Order.class);
    }

    @Test
    void generateWeeklySalesReport() {

        final LocalDate today = LocalDate.now();
        final ValueRange range = today.range(ChronoField.DAY_OF_WEEK);

        for (int i = 1; i <= range.getMaximum(); i++) {
            final LocalDate date = today.with(WeekFields.of(DayOfWeek.SUNDAY, 7).dayOfWeek(), i);
            createOrder(date, "coffee", BigDecimal.valueOf(50), 5);
            createOrder(date, "tea", BigDecimal.valueOf(35), 5);
        }

        final RangedSalesReport results = salesReportService.generateWeeklySalesReport("client");

        assertThat(results.getTotalSales().getSalesTotal()).isEqualByComparingTo(String.valueOf((50 + 35) * 5 * 7));
        assertThat(results.getSalesByRange()).hasSize(7);
        assertThat(results.getSalesByProduct()).hasSize(2);

        assertThat(results.getSalesByProduct()).allSatisfy(byProduct -> {
            assertThat(byProduct.getProductName()).isNotNull();
            assertThat(byProduct.getProductSales()).isNotZero();
            assertThat(byProduct.getPercentage()).isNotZero();
        });

        System.out.println(results);
    }

    @Test
    void generateSalesDistribution() {

        final LocalDate today = LocalDate.now();

        for (int i = 1; i <= 52; i++) {
            createOrder(today.with(ChronoField.ALIGNED_WEEK_OF_YEAR, i));
        }

        final SalesDistribution salesDistribution = salesReportService.generateSalesDistribution("client");

        assertThat(salesDistribution.getSalesByMonth()).hasSize(12);
        final BigDecimal sumOfMonthlySales = salesDistribution.getSalesByMonth().stream().map(SalesDistribution.MonthlySales::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumOfMonthlySales).isEqualByComparingTo(BigDecimal.valueOf(500 * 52));

        assertThat(salesDistribution.getSalesByWeek()).hasSize(52);
    }

    @Test
    void generateSalesProgress() {

        final LocalDate today = LocalDate.now();
        final ValueRange range = today.range(ChronoField.DAY_OF_MONTH);

        for (int i = 1; i <= range.getMaximum(); i++) {
            createOrder(today.withDayOfMonth(i));
        }

        final SalesProgress salesProgress = salesReportService.generateSalesProgress("client");

        assertThat(salesProgress.getDailySalesProgress()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(salesProgress.getWeeklySalesProgress()).isEqualByComparingTo(BigDecimal.valueOf(500 * 7));
        assertThat(salesProgress.getMonthlySalesProgress()).isEqualByComparingTo(BigDecimal.valueOf(500 * range.getMaximum()));
    }

    private void createOrder(final LocalDate orderDate, String productName, BigDecimal price, int quantity) {

        final ProductSnapshot productSnapshot = new ProductSnapshot(null, productName, null, price, null);
        this.createOrder(orderDate, productSnapshot, quantity);
    }

    private void createOrder(final LocalDate orderDate) {
        this.createOrder(orderDate, DummyObjects.productSnapshot(), 5);
    }

    private void createOrder(final LocalDate orderDate, ProductSnapshot productSnapshot, int quantity) {

        final OrderSettings newSettings = orderSettings.copy();
        newSettings.setTaxInclusive(true);

        final Order order = new Order("client", newSettings);
        order.addOrderLineItem(productSnapshot, quantity);
        orderService.createOrder(order);

        // use query to update order.modifiedDate to overwrite the dates that are set by Spring MongoDB auditing feature.
        final Query query = new Query(where("id").is(order.getId()));
        final Update update = new Update().set("modifiedDate", Date.valueOf(orderDate));
        mongoTemplate.updateFirst(query, update, Order.class);
    }
}