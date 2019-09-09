package io.nextpos.membership.data;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface MembershipRepository extends PagingAndSortingRepository<Membership, String> {

    Optional<Membership> findByClientIdAndMobileNumber(String clientId, String mobileNumber);
}
