package io.nextpos.membership.service;

import io.nextpos.membership.data.Membership;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@Transactional
class MembershipServiceImplTest {

    @Autowired
    private MembershipService membershipService;


    @Test
    void saveMembership() {

        final Membership membership = new Membership("client", "Joe", "0988120232");

        membershipService.saveMembership(membership);

        assertThat(membership.getId()).isNotNull();
        assertThat(membership.getMembershipStatus()).isEqualTo(Membership.MembershipStatus.ACTIVE);

        assertThatCode(() -> membershipService.getMembership(membership.getId())).doesNotThrowAnyException();

        membershipService.getMembershipByMobile("client", "0988120232").orElseThrow();
    }
}