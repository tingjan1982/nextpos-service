package io.nextpos.membership.data;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends PagingAndSortingRepository<Membership, String> {

    Optional<Membership> findByClientIdAndPhoneNumber(String clientId, String phoneNumber);

    List<Membership> findAllByClientId(String clientId);
}
