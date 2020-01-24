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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@SpringBootTest
@Transactional
class SalesReportServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesReportServiceImplTest.class);

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
    void generateWeeklySalesReport_WeekRangeType() {

        final LocalDate today = LocalDate.now();
        final ValueRange range = today.range(ChronoField.DAY_OF_WEEK);

        for (int i = 1; i <= range.getMaximum(); i++) {
            final LocalDate date = today.with(WeekFields.of(DayOfWeek.SUNDAY, 7).dayOfWeek(), i);
            createOrder(date, "coffee", BigDecimal.valueOf(50), 5);
            createOrder(date, "tea", BigDecimal.valueOf(35), 5);
        }

        final RangedSalesReport results = salesReportService.generateWeeklySalesReport("client", RangedSalesReport.RangeType.WEEK);

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
    void generateWeeklySalesReport_WithEmptyData() {

        final RangedSalesReport results = salesReportService.generateWeeklySalesReport("client", RangedSalesReport.RangeType.WEEK);

        assertThat(results.getTotalSales().getSalesTotal()).isEqualByComparingTo("0");
        assertThat(results.getSalesByRange()).hasSize(7);
        assertThat(results.getSalesByProduct()).hasSize(0);

        LOGGER.info("{}", results);
    }

    @Test
    void generateWeeklySalesReport_MonthRangeType() {

        final LocalDate today = LocalDate.now();
        final LocalDate lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        for (int i = 1; i <= lastDayOfMonth.getDayOfMonth(); i++) {
            final LocalDate date = today.withDayOfMonth(i);
            createOrder(date, "coffee", BigDecimal.valueOf(50), 5);
            createOrder(date, "tea", BigDecimal.valueOf(35), 5);
        }

        final RangedSalesReport results = salesReportService.generateWeeklySalesReport("client", RangedSalesReport.RangeType.MONTH);

        assertThat(results.getTotalSales().getSalesTotal()).isEqualByComparingTo(String.valueOf((50 + 35) * 5 * lastDayOfMonth.getDayOfMonth()));
        assertThat(results.getSalesByRange()).hasSize(lastDayOfMonth.getDayOfMonth());
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

        final SalesDistribution salesDistribution = salesReportService.generateSalesDistribution("client", LocalDate.now());

        assertThat(salesDistribution.getSalesByMonth()).hasSize(12);
        final BigDecimal sumOfMonthlySales = salesDistribution.getSalesByMonth().stream().map(SalesDistribution.MonthlySales::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumOfMonthlySales).isEqualByComparingTo(BigDecimal.valueOf(500 * 52));

        assertThat(salesDistribution.getSalesByWeek()).hasSize(52);
    }

    @Test
    void generateSalesDistribution_WithEmptyData() {

        final SalesDistribution salesDistribution = salesReportService.generateSalesDistribution("client", LocalDate.now());

        assertThat(salesDistribution.getSalesByMonth()).hasSize(12);
        assertThat(salesDistribution.getSalesByWeek()).hasSize(0);

        LOGGER.info("{}", salesDistribution);
    }

    @Test
    void generateSalesProgress() {

        final LocalDate today = LocalDate.now();
        final LocalDate lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        for (int i = 1; i <= lastDayOfMonth.getDayOfMonth(); i++) {
            createOrder(today.withDayOfMonth(i));
        }

        final SalesProgress salesProgress = salesReportService.generateSalesProgress("client");

        assertThat(salesProgress.getDailySalesProgress()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(salesProgress.getWeeklySalesProgress()).isNotZero(); // for simplicity of not considering week spans across months, just check for non-zero.
        assertThat(salesProgress.getMonthlySalesProgress()).isEqualByComparingTo(BigDecimal.valueOf(500 * lastDayOfMonth.getDayOfMonth()));
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