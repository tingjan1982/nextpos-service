package io.nextpos.script;

import io.nextpos.client.service.ClientService;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.ordermanagement.service.ShiftService;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.reporting.data.RangedSalesReport;
import io.nextpos.reporting.service.SalesReportService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class ManageClientReport {

    private final ClientService clientService;

    private final ShiftService shiftService;

    private final SalesReportService salesReportService;

    @Autowired
    public ManageClientReport(ClientService clientService, ShiftService shiftService, SalesReportService salesReportService) {
        this.clientService = clientService;
        this.shiftService = shiftService;
        this.salesReportService = salesReportService;
    }

    @Test
    void runSalesReport() {

        clientService.getClientByUsername("roncafebar@gmail.com").ifPresent(c -> {

//            final ZonedDateRange dateRange = ZonedDateRangeBuilder.builder(c, DateParameterType.MONTH)
//                    .date(LocalDate.of(2021, 8, 1))
//                    .build();
//
//            shiftService.getShifts(c.getId(), dateRange).forEach(s -> {
//                System.out.println(s);
//            });
//
            final ZonedDateRange dateRange = ZonedDateRangeBuilder.builder(c, DateParameterType.SHIFT)
                    .shift(shiftService.getShift("612b584dd478ea7f5221fa08")).build();

            final RangedSalesReport report = salesReportService.generateRangedSalesReport(c.getId(), dateRange);
            System.out.println(report);


        });

    }
}
