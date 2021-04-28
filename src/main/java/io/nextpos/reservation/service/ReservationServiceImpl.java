package io.nextpos.reservation.service;

import io.nextpos.client.data.Client;
import io.nextpos.reservation.data.Reservation;
import io.nextpos.reservation.data.ReservationDay;
import io.nextpos.reservation.data.ReservationDayRepository;
import io.nextpos.reservation.data.ReservationRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ChainedTransaction
public class ReservationServiceImpl implements ReservationService {

    private final ReservationDayRepository reservationDayRepository;

    private final ReservationRepository reservationRepository;

    private final TableLayoutService tableLayoutService;

    @Autowired
    public ReservationServiceImpl(ReservationDayRepository reservationDayRepository, ReservationRepository reservationRepository, TableLayoutService tableLayoutService) {
        this.reservationDayRepository = reservationDayRepository;
        this.reservationRepository = reservationRepository;
        this.tableLayoutService = tableLayoutService;
    }


    @Override
    public Reservation saveReservation(Client client, Reservation reservation) {

        this.cancelExistingReservation(reservation);

        final LocalDateTime reservationLocalDt = DateTimeUtil.toLocalDateTime(client.getZoneId(), reservation.getReservationDate());
        final ReservationDay reservationDay = this.getOrCreateReservationCapacity(client.getId(), reservationLocalDt.toLocalDate());
        final List<TableLayout.TableDetails> allTables = tableLayoutService.getTableLayouts(client).stream()
                .flatMap(tl -> tl.getTables().stream())
                .collect(Collectors.toList());

        reservationDay.allocateTimeSlot(reservationLocalDt.toLocalTime(), allTables);

        reservationDay.addReservation(reservation, reservationLocalDt.toLocalTime());

        reservationDayRepository.save(reservationDay);

        return reservationRepository.save(reservation);
    }

    private void cancelExistingReservation(Reservation reservation) {

        final ReservationDay reservationDay = reservation.getCurrentReservationDay();

        if (reservationDay != null) {
            reservationDay.cancelReservation(reservation);
        }
    }

    private ReservationDay getOrCreateReservationCapacity(String clientId, LocalDate reservationDate) {

        return reservationDayRepository.findByClientIdAndDate(clientId, reservationDate).orElseGet(() -> {
            return reservationDayRepository.save(new ReservationDay(clientId, reservationDate));
        });
    }

    @Override
    public Reservation getReservation(String id) {
        return reservationRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Reservation.class);
        });
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
}
