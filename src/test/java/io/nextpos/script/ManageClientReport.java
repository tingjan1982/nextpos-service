package io.nextpos.script;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderCriteria;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordermanagement.service.ShiftService;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.reporting.data.RangedSalesReport;
import io.nextpos.reporting.service.SalesReportService;
import io.nextpos.timecard.data.TimeCardReport;
import io.nextpos.timecard.service.TimeCardReportService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class ManageClientReport {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManageClientReport.class);

    private final ClientService clientService;

    private final ShiftService shiftService;

    private final OrderService orderService;

    private final SalesReportService salesReportService;

    private final TimeCardReportService timeCardReportService;

    @Autowired
    public ManageClientReport(ClientService clientService, ShiftService shiftService, OrderService orderService, SalesReportService salesReportService, TimeCardReportService timeCardReportService) {
        this.clientService = clientService;
        this.shiftService = shiftService;
        this.orderService = orderService;
        this.salesReportService = salesReportService;
        this.timeCardReportService = timeCardReportService;
    }

    @Test
    void runSalesReport() {

        clientService.getClientByUsername("ronandcompanytainan@gmail.com").ifPresent(c -> {

            final ZonedDateRange dateRange = this.dateRange(c);
//            StopWatch sw = new StopWatch();
//            sw.start();
            final RangedSalesReport report = salesReportService.generateRangedSalesReport(c.getId(), dateRange);
            //report.getSalesByProduct().forEach(sp -> System.out.println(sp.getSalesQuantity()));
//            sw.stop();
//            System.out.println(sw.prettyPrint());
//            System.out.println(sw.getTotalTimeMillis());
            //System.out.println(report);


            long count = orderService.getOrders(c, dateRange, OrderCriteria.instance().orderState(Order.OrderState.COMPLETED)).stream()
                    .flatMap(o -> o.getOrderLineItems().stream())
                    //.peek(li -> System.out.println(li.getProductSnapshot().getName() + ":" + li.getProductSnapshot().getId()))
                    .filter(li -> li.getProductSnapshot().getId().equals("6c86e20c-b7fc-4c40-897c-4a0ef22b7639"))
                    .mapToInt(OrderLineItem::getQuantity)
                    .sum();

            System.out.println(count);

        });

    }

    private ZonedDateRange dateRange(Client client) {

        final ZonedDateRange dateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.RANGE)
                .dateRange(LocalDateTime.of(2022, 6, 10, 12, 0),
                        LocalDateTime.of(2022, 6, 11, 5, 59))
                .build();

        LOGGER.info("{}", dateRange);

        return dateRange;
    }

    @Test
    void getTimeCardReport() {

        clientService.getClientByUsername("ronandcompanytainan@gmail.com").ifPresent(c -> {
            final TimeCardReport timeCardReport = timeCardReportService.getTimeCardReport(c, YearMonth.of(2021, 9));
            System.out.println(timeCardReport);


            timeCardReport.getUserTimeCards().forEach(us -> {
                System.out.printf("%s: %s, %s\n", us.getDisplayName(), us.getHours(), us.getMinutes());
            });

        });
    }
}
