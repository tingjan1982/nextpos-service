package io.nextpos.membership.service;

import io.nextpos.membership.data.Membership;

import java.util.Optional;

public interface MembershipService {

    Membership saveMembership(Membership membership);

    Membership getMembership(String id);

    Optional<Membership> getMembershipByMobile(final String clientId, String mobile);
}
