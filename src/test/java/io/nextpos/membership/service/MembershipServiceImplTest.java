package io.nextpos.membership.service;

import io.nextpos.client.data.Client;
import io.nextpos.membership.data.Membership;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@Transactional
class MembershipServiceImplTest {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private Client client;


    @Test
    void saveMembership() {

        final Membership membership = new Membership(client.getId(), "Joe", "0988120232");

        membershipService.saveMembership(membership);

        assertThat(membership.getId()).isNotNull();
        assertThat(membership.getMembershipStatus()).isEqualTo(Membership.MembershipStatus.ACTIVE);

        assertThatCode(() -> membershipService.getMembership(membership.getId())).doesNotThrowAnyException();

        membershipService.getMembershipByPhoneNumber(client.getId(), "0988120232").orElseThrow();

        assertThat(membershipService.getMemberships(client)).hasSize(1);

        membershipService.deleteMembership(membership);

        assertThat(membershipService.getMemberships(client)).isEmpty();
    }
}