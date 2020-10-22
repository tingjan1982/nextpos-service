package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderRepository;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.reporting.data.RangedSalesReport;
import io.nextpos.reporting.data.SalesDistribution;
import io.nextpos.reporting.data.SalesProgress;
import io.nextpos.shared.DummyObjects;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private OrderRepository orderRepository;

    @Autowired
    private OrderSettings orderSettings;

    @Autowired
    private MongoTemplate mongoTemplate;

    private Client client;

    @BeforeEach
    void setup() {
        client = DummyObjects.dummyClient();
    }

    @AfterEach
    void removeData() {
        mongoTemplate.findAllAndRemove(new Query(), Order.class);
    }

    @Test
    void generateRangedSalesReport_WeekRangeType() {

        final LocalDate today = LocalDate.now();
        final ValueRange range = today.range(ChronoField.DAY_OF_WEEK);

        for (int i = 1; i <= range.getMaximum(); i++) {
            final LocalDate date = today.with(WeekFields.of(DayOfWeek.MONDAY, 7).dayOfWeek(), i);
            createOrder(date, "coffee", BigDecimal.valueOf(50), 5);
            createOrder(date, "tea", BigDecimal.valueOf(35), 5);
        }

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.WEEK).build();
        final RangedSalesReport results = salesReportService.generateRangedSalesReport(client.getClientName(), zonedDateRange);

        assertThat(results.getTotalSales().getSalesTotal()).isEqualByComparingTo("3272.50"); // (50 + 35) * 5 * 7 * 1.1
        assertThat(results.getSalesByRange()).hasSize(7);
        assertThat(results.getSalesByRange()).allSatisfy(sales -> assertThat(sales.getTotal()).isEqualByComparingTo("467.5")); // (50 + 35) * 5 * 1.1
        assertThat(results.getSalesByProduct()).hasSize(2);

        assertThat(results.getSalesByProduct()).allSatisfy(byProduct -> {
            assertThat(byProduct.getProductName()).isNotNull();
            assertThat(byProduct.getProductSales()).isNotZero();
            assertThat(byProduct.getPercentage()).isNotZero();
        });

        final RangedSalesReport salesRankingReport = salesReportService.generateSalesRankingReport(client.getClientName(), zonedDateRange, "default-id");

        LOGGER.info("{}", salesRankingReport);
    }

    @Test
    void generateRangedSalesReport_WithEmptyData() {

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.WEEK).build();
        final RangedSalesReport results = salesReportService.generateRangedSalesReport(client.getClientName(), zonedDateRange);

        assertThat(results.getTotalSales().getSalesTotal()).isEqualByComparingTo("0");
        assertThat(results.getSalesByRange()).hasSize(7);
        assertThat(results.getSalesByProduct()).hasSize(0);

        LOGGER.info("{}", results);
    }

    @Test
    void generateRangedSalesReport_MonthRangeType() {

        final LocalDate today = LocalDate.now();
        final LocalDate lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal expectedSalesTotal = BigDecimal.ZERO;

        for (int i = 1; i <= lastDayOfMonth.getDayOfMonth(); i++) {
            final LocalDate date = today.withDayOfMonth(i);
            Order order1 = createOrder(date, "coffee", BigDecimal.valueOf(50), 5);
            Order order2 = createOrder(date, "tea", BigDecimal.valueOf(35), 5);

            expectedSalesTotal = expectedSalesTotal.add(order1.getOrderTotal());
            expectedSalesTotal = expectedSalesTotal.add(order2.getOrderTotal());
        }

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH).build();
        final RangedSalesReport results = salesReportService.generateRangedSalesReport(client.getClientName(), zonedDateRange);

        assertThat(results.getTotalSales().getSalesTotal()).isEqualByComparingTo(expectedSalesTotal); // (50 + 35) * 5 * lastDayOfMonth.getDayOfMonth() * 1.1;
        assertThat(results.getSalesByRange()).hasSize(lastDayOfMonth.getDayOfMonth());
        assertThat(results.getSalesByProduct()).hasSize(2);

        assertThat(results.getSalesByProduct()).allSatisfy(byProduct -> {
            assertThat(byProduct.getProductName()).isNotNull();
            assertThat(byProduct.getProductSales()).isNotZero();
            assertThat(byProduct.getPercentage()).isNotZero();
        });
    }

    @Test
    void generateRangedSalesReport_CustomRangeType() {

        final LocalDate today = LocalDate.now();
        final LocalDate lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        for (int i = 1; i <= lastDayOfMonth.getDayOfMonth(); i++) {
            final LocalDate date = today.withDayOfMonth(i);
            createOrder(date, "coffee", BigDecimal.valueOf(50), 5);
            createOrder(date, "tea", BigDecimal.valueOf(35), 5);
        }

        createDeletedOrder(today.withDayOfMonth(15)); // since this is a deleted order, it should not be included in the sales report.

        final LocalDateTime fromDate = LocalDate.now().withDayOfMonth(10).atStartOfDay();
        final LocalDateTime toDate = LocalDate.now().withDayOfMonth(19).atTime(23, 59, 59);

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.CUSTOM)
                .dateRange(fromDate, toDate)
                .build();

        final RangedSalesReport results = salesReportService.generateRangedSalesReport(client.getClientName(), zonedDateRange);

        assertThat(results.getTotalSales().getSalesTotal()).isEqualByComparingTo(String.valueOf((50 + 35) * 5 * 10 * 1.1));
        assertThat(results.getSalesByRange()).hasSize(10);
        assertThat(results.getSalesByProduct()).hasSize(2);

        assertThat(results.getSalesByProduct()).allSatisfy(byProduct -> {
            assertThat(byProduct.getProductName()).isNotNull();
            assertThat(byProduct.getProductSales()).isNotZero();
            assertThat(byProduct.getPercentage()).isNotZero();
        });

        LOGGER.info("{}", results);
    }

    @Test
    void generateSalesDistribution() {

        final LocalDate today = LocalDate.now();

        for (int i = 1; i <= 52; i++) {
            createOrder(today.with(ChronoField.ALIGNED_WEEK_OF_YEAR, i));
        }

        final SalesDistribution salesDistribution = salesReportService.generateSalesDistribution(client.getClientName(), client.getZoneId(), LocalDate.now());

        assertThat(salesDistribution.getSalesByMonth()).hasSize(12);
        final BigDecimal sumOfMonthlySales = salesDistribution.getSalesByMonth().stream().map(SalesDistribution.MonthlySales::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumOfMonthlySales).isCloseTo(BigDecimal.valueOf(500 * 52 * 1.1), Offset.offset(new BigDecimal("0.1")));

        assertThat(salesDistribution.getSalesByWeek()).hasSize(52);
        assertThat(salesDistribution.getSalesByWeek()).allSatisfy(sales -> {
            assertThat(sales.getTotal()).isCloseTo(BigDecimal.valueOf(500 * 1.1), Offset.offset(new BigDecimal("0.1")));
        });
    }

    @Test
    void generateSalesDistribution_WithEmptyData() {

        final SalesDistribution salesDistribution = salesReportService.generateSalesDistribution("client", client.getZoneId(), LocalDate.now());

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

        final SalesProgress salesProgress = salesReportService.generateSalesProgress(client.getClientName());

        assertThat(salesProgress.getDailySalesProgress()).isEqualByComparingTo(BigDecimal.valueOf(550));
        assertThat(salesProgress.getWeeklySalesProgress()).isNotZero(); // for simplicity of not considering week spans across months, just check for non-zero.
        assertThat(salesProgress.getMonthlySalesProgress()).isEqualByComparingTo(BigDecimal.valueOf(550 * lastDayOfMonth.getDayOfMonth()));
    }

    private Order createOrder(final LocalDate orderDate, String productName, BigDecimal price, int quantity) {

        final ProductSnapshot productSnapshot = new ProductSnapshot(null, productName, null, price, null);
        productSnapshot.setLabelInformation("default-id", "default");

        return this.createOrder(orderDate, productSnapshot, quantity, false);
    }

    private void createOrder(final LocalDate orderDate) {
        this.createOrder(orderDate, DummyObjects.productSnapshot(), 5, false);
    }

    private void createDeletedOrder(final LocalDate orderDate) {
        this.createOrder(orderDate, DummyObjects.productSnapshot(), 5, true);
    }

    private Order createOrder(final LocalDate orderDate, ProductSnapshot productSnapshot, int quantity, boolean deleted) {

        final OrderSettings newSettings = orderSettings.copy();
        newSettings.setTaxInclusive(true);

        final Order order = new Order(client.getClientName(), newSettings);
        order.addOrderLineItem(productSnapshot, quantity);

        if (deleted) {
            order.setState(Order.OrderState.DELETED);
        }

        final Order createdOrder = orderService.createOrder(order);

        // use query to update order.modifiedDate to overwrite the dates that are set by Spring MongoDB auditing feature.
        final Query query = new Query(where("id").is(order.getId()));
        final Update update = new Update().set("createdDate", Date.valueOf(orderDate));
        mongoTemplate.updateFirst(query, update, Order.class);

        return createdOrder;
    }
}