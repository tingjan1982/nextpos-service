package io.nextpos.reservation.service;

import io.nextpos.client.data.Client;
import io.nextpos.notification.data.SmsDetails;
import io.nextpos.notification.service.NotificationService;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.reservation.data.*;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@ChainedTransaction
public class ReservationServiceImpl implements ReservationService {

    private final ReservationDayRepository reservationDayRepository;

    private final ReservationRepository reservationRepository;

    private final ReservationSettingsRepository reservationSettingsRepository;

    private final OrderService orderService;

    private final TableLayoutService tableLayoutService;

    private final NotificationService notificationService;

    private final SettingsService settingsService;

    private final MongoTemplate mongoTemplate;

    private final String reservationUrl;

    @Autowired
    public ReservationServiceImpl(ReservationDayRepository reservationDayRepository, ReservationRepository reservationRepository, ReservationSettingsRepository reservationSettingsRepository, OrderService orderService, TableLayoutService tableLayoutService, NotificationService notificationService, SettingsService settingsService, MongoTemplate mongoTemplate, @Value("${reservation.url}") String reservationUrl) {
        this.reservationDayRepository = reservationDayRepository;
        this.reservationRepository = reservationRepository;
        this.reservationSettingsRepository = reservationSettingsRepository;
        this.orderService = orderService;
        this.tableLayoutService = tableLayoutService;
        this.notificationService = notificationService;
        this.settingsService = settingsService;
        this.mongoTemplate = mongoTemplate;
        this.reservationUrl = reservationUrl;
    }


    @Override
    public Reservation saveReservation(Client client, Reservation reservation) {

        final LocalDateTime reservationStartDt = DateTimeUtil.toLocalDateTime(client.getZoneId(), reservation.getStartDate());
        final ReservationDay reservationDay = this.getOrCreateReservationCapacity(client.getId(), reservationStartDt.toLocalDate());

        if (!reservationDay.isReservable()) {
            throw new BusinessLogicException("message.notReservable", "Reservation date is marked as non-reservable: " + reservationStartDt.toLocalDate());
        }

        // mark as cancelled first to accommodate case of updating existing reservation
        final Reservation.ReservationStatus previousStatus = reservation.getStatus();

        if (!reservation.isNew()) {
            this.cancelReservation(reservation);
        }

        final ReservationSettings reservationSettings = this.getOrCreateReservationSettings(client);
        final LocalDateTime reservationEndDt = reservationSettings.getEndDate(reservationStartDt);
        reservation.setEndDate(DateTimeUtil.toDate(client.getZoneId(), reservationEndDt));

        final List<TableLayout.TableDetails> availableTables = this.getAvailableReservableTables(client, reservationStartDt);

        if (CollectionUtils.isEmpty(availableTables)) {
            throw new BusinessLogicException("message.notAvailable", "The selected time slot has no empty table");
        }

        reservation.setStatus(previousStatus);

        final Reservation saved = reservationRepository.save(reservation);
        //this.sendNotification(client, saved);

        return saved;
    }

    private ReservationDay getOrCreateReservationCapacity(String clientId, LocalDate reservationDate) {

        return reservationDayRepository.findByClientIdAndDate(clientId, reservationDate).orElseGet(() -> {
            return reservationDayRepository.save(new ReservationDay(clientId, reservationDate));
        });
    }

    @Override
    public void sendReservationNotification(Client client, Reservation reservation) {

        final CountrySettings countrySettings = settingsService.getCountrySettings(client.getCountryCode());
        String dateTime = DateTimeUtil.formatDate(client.getZoneId(), reservation.getStartDate(), "MM/dd HH:mm");
        String reservationLink = reservationUrl + "/r/" + reservation.getId();
        final String formattedNumber = countrySettings.formatPhoneNumber(reservation.getPhoneNumber());

        String message = String.format("%s %s: %s", client.getClientName(), dateTime, reservationLink);
        SmsDetails smsDetails = new SmsDetails(reservation.getClientId(), formattedNumber, message);

        notificationService.sendSimpleNotification(smsDetails);
    }

    @Override
    public List<TableLayout.TableDetails> getAvailableReservableTables(Client client, LocalDateTime reservationTime) {

        return this.getAvailableReservableTables(client, reservationTime, null);
    }

    @Override
    public List<TableLayout.TableDetails> getAvailableReservableTables(Client client, LocalDateTime reservationTime, String reservationId) {

        final ReservationSettings reservationSettings = this.getOrCreateReservationSettings(client);

        final List<String> bookedTables = this.getReservationsByDateRange(client, reservationTime, reservationSettings.getEndDate(reservationTime)).stream()
                .filter(r -> !Objects.equals(r.getId(), reservationId))
                .flatMap(r -> r.getTableAllocations().stream())
                .map(Reservation.TableAllocation::getTableId)
                .collect(Collectors.toList());

//        final LocalDate today = DateTimeUtil.toLocalDate(client.getZoneId(), new Date());
//
//        if (reservationTime.toLocalDate().isEqual(today)) {
//            final List<String> inflightTables = orderService.getInStoreInFlightOrders(client.getId()).stream()
//                    .flatMap(o -> o.getTables().stream())
//                    .map(Order.TableInfo::getTableId)
//                    .collect(Collectors.toList());
//
//            bookedTables.addAll(inflightTables);
//        }

        return tableLayoutService.getTableLayouts(client).stream()
                .flatMap(tl -> tl.getTables().stream())
                .filter(td -> !bookedTables.contains(td.getId()))
                .filter(td -> !reservationSettings.getNonReservableTables().contains(td.getId()))
                .collect(Collectors.toList());
    }

    private ReservationSettings getOrCreateReservationSettings(Client client) {
        return reservationSettingsRepository.findById(client.getId()).orElseGet(() -> {
            return reservationSettingsRepository.save(new ReservationSettings(client.getId()));
        });
    }

    @Override
    public Reservation getReservation(String id) {
        return reservationRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Reservation.class);
        });
    }

    @Override
    public void confirmReservation(Reservation reservation) {
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
    }

    @Override
    public void seatReservation(Reservation reservation) {
        reservation.setStatus(Reservation.ReservationStatus.SEATED);
        reservationRepository.save(reservation);
    }

    @Override
    public void cancelReservation(Reservation reservation) {
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    @Override
    public void deleteReservation(String id) {
        reservationRepository.deleteById(id);
    }

    @Override
    public ReservationDay getReservationDay(Client client, LocalDate localDate) {

        return reservationDayRepository.findByClientIdAndDate(client.getId(), localDate).orElseThrow(() -> {
            throw new ObjectNotFoundException(localDate.toString(), ReservationDay.class);
        });
    }

    @Override
    public List<Reservation> getReservationsByDateRange(Client client, LocalDateTime startDt, LocalDateTime endDt) {

        final Date startDate = DateTimeUtil.toDate(client.getZoneId(), startDt);
        final Date endDate = DateTimeUtil.toDate(client.getZoneId(), endDt);

        Query query = Query.query(new Criteria().andOperator(
                where("clientId").is(client.getId())
                        .and("status").ne(Reservation.ReservationStatus.CANCELLED),
                new Criteria().orOperator(
                        where("startDate").gte(startDate).lt(endDate), // less than end date to book right after.
                        where("endDate").gt(startDate).lte(endDate)) // greater than start date to book right before.
                )
        );

        return mongoTemplate.find(query, Reservation.class);
    }

    @Override
    public List<Reservation> getReservationsByDateRange(Client client, YearMonth yearMonth) {

        final Date startDate = DateTimeUtil.toDate(client.getZoneId(), yearMonth.atDay(1).atStartOfDay());
        final Date endDate = DateTimeUtil.toDate(client.getZoneId(), yearMonth.atEndOfMonth().atTime(23, 59, 59));

        return this.getReservationsByDateRange(client, startDate, endDate, null);
    }

    @Override
    public List<Reservation> getReservationsByDateRange(Client client, Date startDate, Date endDate, List<Reservation.ReservationStatus> statuses) {

        Query query = new Query().with(Sort.by(Sort.Order.asc("startDate")))
                .addCriteria(where("clientId").is(client.getId())
                        .and("startDate").gte(startDate).lte(endDate));

        if (!CollectionUtils.isEmpty(statuses)) {
            query.addCriteria(where("status").in(statuses));
        }

        return mongoTemplate.find(query, Reservation.class);
    }

    @Override
    public List<Reservation> getReservationsByDateAndStatus(Client client, LocalDate reservationDate, Reservation.ReservationStatus reservationStatus) {

        Date startDate = DateTimeUtil.toDate(client.getZoneId(), reservationDate.atStartOfDay());
        Date endDate = DateTimeUtil.toDate(client.getZoneId(), reservationDate.atTime(23, 59, 59));

        Query query = new Query().with(Sort.by(Sort.Order.asc("startDate")))
                .addCriteria(where("clientId").is(client.getId())
                        .and("startDate").gte(startDate).lte(endDate));

        if (reservationStatus != null) {
            query.addCriteria(where("status").is(reservationStatus));
        }

        final List<Reservation> reservations = mongoTemplate.find(query, Reservation.class);
        reservations.sort(Reservation.getComparator());

        return reservations;
    }

    @Override
    public Reservation delayReservation(Client client, Reservation reservation, long minutesToDelay) {

        final LocalDateTime reservationDt = DateTimeUtil.toLocalDateTime(client.getZoneId(), reservation.getStartDate());
        final LocalDateTime delayedReservationDt = reservationDt.plusMinutes(minutesToDelay);

        reservation.setStartDate(DateTimeUtil.toDate(client.getZoneId(), delayedReservationDt));

        return reservationRepository.save(reservation);
    }
}
