package io.nextpos.ordermanagement.data;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Date;
import java.util.Optional;

public interface ShiftRepository extends PagingAndSortingRepository<Shift, String> {

    Optional<Shift> findByClientIdAndShiftStatus(String clientId, Shift.ShiftStatus shiftStatus);

    Optional<Shift> findFirstByClientIdOrderByCreatedDateDesc(String clientId);

    Page<Shift> findAllByClientIdAndStartTimestampGreaterThanEqual(String clientId, Date date, PageRequest pageRequest);
}
