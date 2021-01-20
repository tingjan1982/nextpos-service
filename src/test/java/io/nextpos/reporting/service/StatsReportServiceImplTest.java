package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderRepository;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.reporting.data.CustomerStatsReport;
import io.nextpos.reporting.data.CustomerTrafficReport;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.shared.DummyObjects;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@SpringBootTest
@Transactional
class StatsReportServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatsReportServiceImplTest.class);

    @Autowired
    private StatsReportService statsReportService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private OrderSettings orderSettings;

    private Client client;

    @BeforeEach
    void setup() {
        client = DummyObjects.dummyClient();
        client.setId("client");
    }

    @AfterEach
    void cleanData() {
        orderRepository.deleteAll();
    }

    @Test
    @WithMockUser
    void generateCustomerTrafficReport() {

        final LocalDateTime today = LocalDateTime.now();

        for (int i = 0; i <= 23; i++) {
            final LocalDateTime date = today.with(ChronoField.HOUR_OF_DAY, i);
            createOrder(date, Order.OrderType.IN_STORE, Order.DemographicData.AgeGroup.TWENTIES, Order.DemographicData.VisitFrequency.FIRST_TIME, 1, 1, 1);
            createOrder(date, Order.OrderType.TAKE_OUT, Order.DemographicData.AgeGroup.THIRTIES, Order.DemographicData.VisitFrequency.MORE_THAN_THREE, 1, 1, 1);
        }

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH).build();
        final CustomerTrafficReport results = statsReportService.generateCustomerTrafficReport("client", zonedDateRange);

        assertThat(results.getTotalCountObject()).isNotEmpty();
        results.getTotalCountObject().ifPresent(count -> {
            assertThat(count.getOrderCount()).isEqualTo(48);
            assertThat(count.getMaleCount()).isEqualTo(48);
            assertThat(count.getMalePercentage()).isCloseTo(BigDecimal.valueOf(33), within(BigDecimal.ONE));
            assertThat(count.getFemaleCount()).isEqualTo(48);
            assertThat(count.getFemalePercentage()).isCloseTo(BigDecimal.valueOf(33), within(BigDecimal.ONE));
            assertThat(count.getKidCount()).isEqualTo(48);
            assertThat(count.getKidPercentage()).isCloseTo(BigDecimal.valueOf(33), within(BigDecimal.ONE));
            assertThat(count.getCustomerCount()).isEqualTo(48 * 3);
        });

        assertThat(results.getOrdersByHour()).hasSize(24);

        assertThat(results.getOrdersByHour()).allSatisfy(cc -> {
            assertThat(cc.getOrderCount()).isEqualTo(2);
            assertThat(cc.getCustomerCount()).isEqualTo(6);
        });

        assertThat(results.getOrdersByType()).hasSize(Order.OrderType.values().length);
        assertThat(results.getOrdersByType()).anySatisfy(order -> {
            assertThat(order.getOrderCount()).isEqualTo(24);
            assertThat(order.getPercentage()).isCloseTo(BigDecimal.valueOf(50), within(BigDecimal.ONE));
        });
        assertThat(results.getOrdersByAgeGroup()).hasSize(Order.DemographicData.AgeGroup.values().length);
        assertThat(results.getOrdersByVisitFrequency()).hasSize(Order.DemographicData.VisitFrequency.values().length);

        LOGGER.info("{}", results);
    }

    @Test
    void generateCustomerTrafficReport_EnhanceResult() {

        createOrder(LocalDateTime.now(), Order.OrderType.IN_STORE, Order.DemographicData.AgeGroup.TWENTIES, Order.DemographicData.VisitFrequency.FIRST_TIME, 1, 1, 1);

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH).build();
        final CustomerTrafficReport results = statsReportService.generateCustomerTrafficReport("client", zonedDateRange);

        assertThat(results.getOrdersByHour()).hasSize(24);
        assertThat(results.getOrdersByType()).hasSize(Order.OrderType.values().length);
        assertThat(results.getOrdersByAgeGroup()).hasSize(Order.DemographicData.AgeGroup.values().length);
        assertThat(results.getOrdersByVisitFrequency()).hasSize(Order.DemographicData.VisitFrequency.values().length);
    }

    @Test
    void generateEmptyCustomerTrafficReport() {

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH).build();
        final CustomerTrafficReport results = statsReportService.generateCustomerTrafficReport("client", zonedDateRange);

        assertThat(results.getTotalCountObject()).isEmpty();
        assertThat(results.getOrdersByHour()).isEmpty();
        assertThat(results.getOrdersByType()).isEmpty();
        assertThat(results.getOrdersByAgeGroup()).isEmpty();
        assertThat(results.getOrdersByVisitFrequency()).isEmpty();
    }

    private void createOrder(final LocalDateTime orderDate,
                             final Order.OrderType orderType,
                             final Order.DemographicData.AgeGroup ageGroup,
                             final Order.DemographicData.VisitFrequency visitFrequency,
                             final int male, final int female, final int kid) {

        final OrderSettings newSettings = orderSettings.copy();
        newSettings.setTaxInclusive(true);

        final Order order = new Order("client", newSettings);
        order.setOrderType(orderType);
        final Order.DemographicData demographicData = new Order.DemographicData();
        demographicData.setAgeGroup(ageGroup);
        demographicData.setVisitFrequency(visitFrequency);
        demographicData.setMale(male);
        demographicData.setFemale(female);
        demographicData.setKid(kid);
        order.setDemographicData(demographicData);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);

        orderService.createOrder(order);

        // use query to update order.modifiedDate to overwrite the dates that are set by Spring MongoDB auditing feature.
        final Query query = new Query(where("id").is(order.getId()));


        final Update update = new Update().set("createdDate", Date.from(orderDate.atZone(ZoneOffset.systemDefault()).toInstant()));
        mongoTemplate.updateFirst(query, update, Order.class);
    }

    @Test
    void generateCustomerStatsReport() {

        final LocalDate today = LocalDate.now();
        final LocalDate lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        for (int i = 1; i <= lastDayOfMonth.getDayOfMonth(); i++) {
            final LocalDate date = today.withDayOfMonth(i);
            createOrder(date, 2, 2, 2);
            createOrder(date, 1, 1, 1);
        }

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH).build();
        final CustomerStatsReport results = statsReportService.generateCustomerStatsReport("client", zonedDateRange);

        assertThat(results.getGroupedCustomerStats()).hasSize(1);

        assertThat(results.getGroupedCustomerStats()).allSatisfy(cc -> {
            assertThat(cc.getCustomerCount()).isEqualTo(279);
            assertThat(cc.getAverageSpending()).isCloseTo(BigDecimal.valueOf(24), within(BigDecimal.valueOf(1)));
        });
    }

    @Test
    void generateEmptyCustomerStatsReport() {

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH).build();
        final CustomerStatsReport results = statsReportService.generateCustomerStatsReport("client", zonedDateRange);

        assertThat(results.getGroupedCustomerStats()).hasSize(0);

        assertThat(results.getGroupedCustomerStats()).allSatisfy(cc -> {
            assertThat(cc.getId()).isNotNull();
            assertThat(cc.getCustomerCount()).isEqualTo(0);
            assertThat(cc.getAverageSpending()).isCloseTo(BigDecimal.valueOf(0), within(BigDecimal.valueOf(1)));
        });
    }

    private void createOrder(final LocalDate orderDate, final int male, final int female, int kid) {

        final OrderSettings newSettings = orderSettings.copy();
        newSettings.setTaxInclusive(true);

        final Order order = new Order("client", newSettings);
        final Order.DemographicData demographicData = new Order.DemographicData();
        demographicData.setMale(male);
        demographicData.setFemale(female);
        demographicData.setKid(kid);
        order.setDemographicData(demographicData);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);

        orderService.createOrder(order);

        // use query to update order.modifiedDate to overwrite the dates that are set by Spring MongoDB auditing feature.
        final Query query = new Query(where("id").is(order.getId()));
        final Update update = new Update().set("createdDate", Date.valueOf(orderDate));
        mongoTemplate.updateFirst(query, update, Order.class);
    }
}