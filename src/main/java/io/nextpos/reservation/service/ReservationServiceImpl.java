package io.nextpos.reservation.service;

import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.reservation.data.*;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
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

    private final MongoTemplate mongoTemplate;

    @Autowired
    public ReservationServiceImpl(ReservationDayRepository reservationDayRepository, ReservationRepository reservationRepository, ReservationSettingsRepository reservationSettingsRepository, OrderService orderService, TableLayoutService tableLayoutService, MongoTemplate mongoTemplate) {
        this.reservationDayRepository = reservationDayRepository;
        this.reservationRepository = reservationRepository;
        this.reservationSettingsRepository = reservationSettingsRepository;
        this.orderService = orderService;
        this.tableLayoutService = tableLayoutService;
        this.mongoTemplate = mongoTemplate;
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
        return reservationRepository.save(reservation);
    }

    private ReservationDay getOrCreateReservationCapacity(String clientId, LocalDate reservationDate) {

        return reservationDayRepository.findByClientIdAndDate(clientId, reservationDate).orElseGet(() -> {
            return reservationDayRepository.save(new ReservationDay(clientId, reservationDate));
        });
    }

    private ReservationSettings getOrCreateReservationSettings(Client client) {
        return reservationSettingsRepository.findById(client.getId()).orElseGet(() -> {
            return reservationSettingsRepository.save(new ReservationSettings(client.getId()));
        });
    }

    @Override
    public List<TableLayout.TableDetails> getAvailableReservableTables(Client client, LocalDateTime reservationTime) {

        final ReservationSettings reservationSettings = this.getOrCreateReservationSettings(client);

        final List<String> bookedTables = this.getReservationsByDateRange(client, reservationTime, reservationSettings.getEndDate(reservationTime)).stream()
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
                .collect(Collectors.toList());
    }

    @Override
    public Reservation getReservation(String id) {
        return reservationRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Reservation.class);
        });
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
    public List<Reservation> getReservationsByDateAndStatus(Client client, LocalDate reservationDate, Reservation.ReservationStatus reservationStatus) {

        Date startDate = DateTimeUtil.toDate(client.getZoneId(), reservationDate.atStartOfDay());
        Date endDate = DateTimeUtil.toDate(client.getZoneId(), reservationDate.atTime(23, 59, 59));

        Query query = new Query().with(Sort.by(Sort.Order.asc("id")))
                .addCriteria(where("clientId").is(client.getId())
                .and("status").is(reservationStatus)
                .and("startDate").gte(startDate).lte(endDate));

        return mongoTemplate.find(query, Reservation.class);
    }
}
