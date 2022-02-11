package io.nextpos.reservation.service;

import io.nextpos.reservation.data.ReservationSettings;
import io.nextpos.reservation.data.ReservationSettingsRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@ChainedTransaction
public class ReservationSettingsServiceImpl implements ReservationSettingsService {

    private final ReservationSettingsRepository reservationSettingsRepository;

    @Autowired
    public ReservationSettingsServiceImpl(ReservationSettingsRepository reservationSettingsRepository) {
        this.reservationSettingsRepository = reservationSettingsRepository;
    }

    @Override
    public ReservationSettings getReservationSettings(String id) {
        return reservationSettingsRepository.findByClientId(id).orElseGet(() -> {
            return reservationSettingsRepository.save(new ReservationSettings(id));
        });
    }

    @Override
    public ReservationSettings getReservationSettingsByReservationKey(String reservationKey) {
        return reservationSettingsRepository.findByReservationKey(reservationKey).orElseThrow(() -> {
            throw new ObjectNotFoundException(reservationKey, ReservationSettings.class);
        });
    }

    @Override
    public ReservationSettings saveReservationSettings(ReservationSettings reservationSettings) {
        return reservationSettingsRepository.save(reservationSettings);
    }
}
