package io.nextpos.membership.service;

import io.nextpos.client.data.Client;
import io.nextpos.membership.data.Membership;

import java.util.List;
import java.util.Optional;

public interface MembershipService {

    Membership saveMembership(Membership membership);

    Membership getMembership(String id);

    Optional<Membership> getMembershipByPhoneNumber(final String clientId, String mobile);

    List<Membership> getMemberships(Client client);

    void deleteMembership(Membership membership);
}
