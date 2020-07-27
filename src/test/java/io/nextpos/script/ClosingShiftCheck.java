package io.nextpos.script;

import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordermanagement.data.ShiftRepository;
import io.nextpos.ordermanagement.service.ShiftService;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

@Disabled
@SpringBootTest
@TestPropertySource(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration")
public class ClosingShiftCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClosingShiftCheck.class);

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private ShiftRepository shiftRepository;

    @Test
    void test() {

        final String clientId = "cli-pK57SUracYZILpXFiCaSg8YLgL6F";
        AtomicInteger diffCount = new AtomicInteger();

        shiftRepository.findAllByClientId(clientId).forEach(s -> {

            final Shift.CloseShiftDetails end = s.getEnd();
            final ClosingShiftTransactionReport closingShiftReport = shiftService.getClosingShiftReport(clientId, s.getId());

            if (!CollectionUtils.isEmpty(closingShiftReport.getOrderSummary())) {
                LOGGER.info("{}", closingShiftReport);

                final BigDecimal computedTotal = closingShiftReport.getOrderSummary().get(0).getOrderTotal();
                final BigDecimal total = end.getClosingShiftReport().getTotalByPaymentMethod().values().stream()
                        .map(ClosingShiftTransactionReport.PaymentMethodTotal::getOrderTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

                final boolean balanced = computedTotal.compareTo(total) == 0;
                LOGGER.info("Shift id: {}, Computed order total: {}, shift total: {}, balanced: {}", s.getId(),
                        computedTotal,
                        total,
                        balanced);

                if (!balanced) {
                    diffCount.incrementAndGet();
                }
            }
        });

        LOGGER.info("Difference count: {}", diffCount);
    }
}
