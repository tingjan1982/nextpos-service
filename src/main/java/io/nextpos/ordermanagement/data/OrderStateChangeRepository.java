package io.nextpos.ordermanagement.data;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface OrderStateChangeRepository extends PagingAndSortingRepository<OrderStateChange, String> {
}
