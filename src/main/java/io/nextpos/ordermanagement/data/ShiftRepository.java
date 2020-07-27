package io.nextpos.ordermanagement.data;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends PagingAndSortingRepository<Shift, String> {

    Optional<Shift> findByClientIdAndShiftStatus(String clientId, Shift.ShiftStatus shiftStatus);

    Optional<Shift> findFirstByClientIdOrderByCreatedDateDesc(String clientId);

    List<Shift> findAllByClientIdAndStart_TimestampBetween(String clientId, Date fromDate, Date toDate, Sort sort);

    List<Shift> findAllByClientId(String clientId);
}
