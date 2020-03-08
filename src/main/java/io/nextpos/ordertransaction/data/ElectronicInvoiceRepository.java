package io.nextpos.ordertransaction.data;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface ElectronicInvoiceRepository extends PagingAndSortingRepository<ElectronicInvoice, String> {
}
