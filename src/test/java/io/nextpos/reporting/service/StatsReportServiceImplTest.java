package io.nextpos.reporting.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.reporting.data.CustomerCountReport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import javax.transaction.Transactional;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
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
    private MongoTemplate mongoTemplate;

    @Autowired
    private OrderSettings orderSettings;


    @Test
    void generateCustomerCountReport() {

        final LocalDate today = LocalDate.now();
        final LocalDate lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        for (int i = 1; i <= lastDayOfMonth.getDayOfMonth(); i++) {
            final LocalDate date = today.withDayOfMonth(i);
            createOrder(date, 2, 2, 2);
        }

        final CustomerCountReport results = statsReportService.generateCustomerCountReport("client", LocalDate.now());

        assertThat(results.getGroupedCustomerCount()).hasSize(lastDayOfMonth.getDayOfMonth());

        assertThat(results.getGroupedCustomerCount()).allSatisfy(cc -> {
            assertThat(cc.getMaleCount()).isEqualTo(2);
            assertThat(cc.getFemaleCount()).isEqualTo(2);
            assertThat(cc.getKidCount()).isEqualTo(2);
            assertThat(cc.getCustomerCount()).isEqualTo(6);
        });
        
        LOGGER.info("{}", results);
    }

    private void createOrder(final LocalDate orderDate, final int male, final int female, int kid) {

        final Order order = new Order("client", orderSettings);
        final Order.DemographicData demographicData = new Order.DemographicData();
        demographicData.setMale(male);
        demographicData.setFemale(female);
        demographicData.setKid(kid);
        order.setDemographicData(demographicData);

        orderService.createOrder(order);

        // use query to update order.modifiedDate to overwrite the dates that are set by Spring MongoDB auditing feature.
        final Query query = new Query(where("id").is(order.getId()));
        final Update update = new Update().set("modifiedDate", Date.valueOf(orderDate));
        mongoTemplate.updateFirst(query, update, Order.class);
    }
}