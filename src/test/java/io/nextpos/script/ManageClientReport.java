package io.nextpos.script;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
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

    private final SalesReportService salesReportService;

    private final TimeCardReportService timeCardReportService;

    @Autowired
    public ManageClientReport(ClientService clientService, ShiftService shiftService, SalesReportService salesReportService, TimeCardReportService timeCardReportService) {
        this.clientService = clientService;
        this.shiftService = shiftService;
        this.salesReportService = salesReportService;
        this.timeCardReportService = timeCardReportService;
    }

    @Test
    void runSalesReport() {

        clientService.getClientByUsername("ronandcompanytainan@gmail.com").ifPresent(c -> {

            final ZonedDateRange dateRange = this.dateRange(c);
            StopWatch sw = new StopWatch();
            sw.start();
            final RangedSalesReport report = salesReportService.generateRangedSalesReport(c.getId(), dateRange);
            sw.stop();
            System.out.println(sw.prettyPrint());
            System.out.println(sw.getTotalTimeMillis());
            //System.out.println(report);
        });

    }

    private ZonedDateRange dateRange(Client client) {

        final ZonedDateRange dateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.RANGE)
                .dateRange(LocalDateTime.of(2022, 1, 1, 0, 0),
                        LocalDateTime.of(2022, 4, 30, 23, 59))
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
