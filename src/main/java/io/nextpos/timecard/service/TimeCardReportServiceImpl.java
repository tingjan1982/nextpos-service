package io.nextpos.timecard.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.service.annotation.MongoTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.timecard.data.TimeCardReport;
import io.nextpos.timecard.data.UserTimeCard;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@MongoTransaction
public class TimeCardReportServiceImpl implements TimeCardReportService {

    private final MongoTemplate mongoTemplate;

    private final ClientService clientService;

    @Autowired
    public TimeCardReportServiceImpl(final MongoTemplate mongoTemplate, final ClientService clientService) {
        this.mongoTemplate = mongoTemplate;
        this.clientService = clientService;
    }

    @Override
    public TimeCardReport getTimeCardReport(final Client client, final YearMonth yearMonth) {

        ProjectionOperation projection = Aggregation.project("clientId")
                .and("username").as("username")
                .and("nickname").as("nickname")
                .and("clockIn").as("clockIn")
                .and("clockOut").as("clockOut")
                .and(context -> Document.parse("{ $dayOfMonth: {date: '$clockIn', timezone: 'Asia/Taipei'} }")).as("day")
                .andExpression("(clockOut - clockIn) / (1000 * 60 * 60)").as("hour");

        final Date fromDate = DateTimeUtil.toDate(client.getZoneId(), yearMonth.atDay(1).atStartOfDay());
        final Date toDate = DateTimeUtil.toDate(client.getZoneId(), yearMonth.atEndOfMonth().atTime(23, 59, 59).plusDays(1));

        final MatchOperation filter = Aggregation.match(Criteria.where("clientId").is(client.getId())
                .and("clockIn").gte(fromDate).lt(toDate));

        final SortOperation sortByClockIn = Aggregation.sort(Sort.Direction.DESC, "clockIn");
        final AggregationOperation userTimeCards = Aggregation.group("username")
                .first("nickname").as("nickname")
                .count().as("totalShifts")
                .sum("hour").as("totalHours");

        final FacetOperation facets = Aggregation.facet(sortByClockIn, userTimeCards).as("userTimeCards");

        final TypedAggregation<UserTimeCard> aggregations = Aggregation.newAggregation(UserTimeCard.class,
                projection,
                filter,
                facets);

        final AggregationResults<TimeCardReport> result = mongoTemplate.aggregate(aggregations, TimeCardReport.class);

        final TimeCardReport timeCardReport = result.getUniqueMappedResult();

        if (timeCardReport != null) {
            enhanceResult(client, timeCardReport);
        }

        return timeCardReport;
    }

    void enhanceResult(final Client client, TimeCardReport timeCardReport) {

        final List<ClientUser> clientUsers = clientService.getClientUsers(client);
        final Map<String, TimeCardReport.UserShift> userShifts = timeCardReport.getUserTimeCards().stream()
                .collect(Collectors.toMap(TimeCardReport.UserShift::getId, t -> t));

        clientUsers.forEach(u -> {
            final TimeCardReport.UserShift emptyUserShift = new TimeCardReport.UserShift(u.getId().getUsername(),
                    u.getNickname(),
                    0,
                    BigDecimal.ZERO);
            userShifts.putIfAbsent(u.getId().getUsername(), emptyUserShift);
        });

        final List<TimeCardReport.UserShift> sortedUserTimeCards = new ArrayList<>(userShifts.values());
        sortedUserTimeCards.sort(Comparator.comparing(TimeCardReport.UserShift::getId));

        timeCardReport.setUserTimeCards(sortedUserTimeCards);
    }
}
