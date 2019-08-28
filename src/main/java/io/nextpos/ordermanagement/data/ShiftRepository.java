package io.nextpos.ordermanagement.data;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface ShiftRepository extends PagingAndSortingRepository<Shift, String> {

    Optional<Shift> findByClientIdAndShiftStatus(String clientId, Shift.ShiftStatus shiftStatus);
}
