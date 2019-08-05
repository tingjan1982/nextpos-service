package io.nextpos.ordertransaction.data;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface OrderTransactionRepository extends PagingAndSortingRepository<OrderTransaction, String> {
}
