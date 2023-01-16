package io.nextpos.script;

import io.nextpos.client.data.ClientUserRepository;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.timecard.data.TimeCardReport;
import io.nextpos.timecard.data.UserTimeCard;
import io.nextpos.timecard.data.UserTimeCardRepository;
import io.nextpos.timecard.service.TimeCardReportService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class UpdateUserTimeCard {

    static {
        System.setProperty("jdk.tls.client.protocols", "TLSv1, TLSv1.1, TLSv1.2");
    }

    private final UserTimeCardRepository userTimeCardRepository;

    private final ClientService clientService;

    private final ClientUserRepository clientUserRepository;

    private final TimeCardReportService timeCardReportService;

    @Autowired
    public UpdateUserTimeCard(UserTimeCardRepository userTimeCardRepository, ClientService clientService, ClientUserRepository clientUserRepository, TimeCardReportService timeCardReportService) {
        this.userTimeCardRepository = userTimeCardRepository;
        this.clientService = clientService;
        this.clientUserRepository = clientUserRepository;
        this.timeCardReportService = timeCardReportService;
    }

    @Test
    void updateUserTimeCard() {

        final AtomicInteger count = new AtomicInteger();

        userTimeCardRepository.findAll().forEach(tc -> {
            count.incrementAndGet();

            clientService.getClient(tc.getClientId()).ifPresentOrElse(c -> {
                clientUserRepository.findByClientAndUsername(c, tc.getUsername()).ifPresentOrElse(cu -> {
                    if (tc.getUsername() == null) {
                        System.out.print("[no username] ");
                    }

                    System.out.printf("username: %s, nickname: %s\n", tc.getUsername(), tc.getNickname());
                }, () -> {
                    userTimeCardRepository.delete(tc);
                    System.out.println("Removed user time card as user is not found: " + tc.getNickname());
                });
                
            }, () -> {
                userTimeCardRepository.delete(tc);
                System.out.println("Removed user time card whose client does not exist: " + tc.getNickname());
            });
        });

        System.out.println("Total records: " + count);
    }

    @Test
    void createOrUpdateTimeCard() {

        clientService.getClientByUsername("ronandcompanytainan@gmail.com").ifPresent(c -> {
            clientUserRepository.findByClientAndNickname(c, "Raid").ifPresent(cu -> {

                /*UserTimeCard timeCard = new UserTimeCard(c.getId(), cu.getUsername(), "Boson");
                ZoneId zoneId = ZoneId.of("Asia/Taipei");
                timeCard.setClockIn(DateTimeUtil.toDate(zoneId, LocalDateTime.of(2022, 12, 4, 18, 0, 0)));
                timeCard.setClockOut(DateTimeUtil.toDate(zoneId, LocalDateTime.of(2022, 12, 5, 2, 0, 0)));
                timeCard.setTimeCardStatus(UserTimeCard.TimeCardStatus.COMPLETE);

                userTimeCardService.saveUserTimeCard(timeCard);*/

                List<UserTimeCard> timeCards = userTimeCardRepository.findAllByClientIdAndUsernameAndClockInDateRange(c.getId(),
                        cu.getUsername(),
                        LocalDate.of(2022, 12, 1),
                        LocalDate.of(2022, 12, 10),
                        Sort.by("clockIn"));

                timeCards.forEach(tc -> {
                    System.out.printf("%s > %s - %s\n",
                            tc.getUsername(),
                            DateTimeUtil.formatDate(ZoneId.of("Asia/Taipei"), tc.getClockIn()),
                            DateTimeUtil.formatDate(ZoneId.of("Asia/Taipei"), tc.getClockOut()));
                });
            });
        });
    }

    @Test
    void timeCardReport() {

        clientService.getClientByUsername("roncafebar@gmail.com").ifPresent(c -> {

            TimeCardReport timeCardReport = timeCardReportService.getTimeCardReport(c, YearMonth.of(2022, 12));
            timeCardReport.getUserTimeCards().forEach(us -> {
                System.out.printf("User: %s, shifts: %d, total: %sH %sM\n", us.getDisplayName(),
                        us.getTotalShifts(),
                        us.getHours(),
                        us.getMinutes());
            });
        });
    }
}
