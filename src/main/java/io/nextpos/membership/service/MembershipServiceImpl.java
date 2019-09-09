package io.nextpos.membership.service;

import io.nextpos.membership.data.Membership;
import io.nextpos.membership.data.MembershipRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class MembershipServiceImpl implements MembershipService {

    private final MembershipRepository membershipRepository;

    @Autowired
    public MembershipServiceImpl(final MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public Membership saveMembership(final Membership membership) {
        return membershipRepository.save(membership);
    }

    @Override
    public Membership getMembership(final String id) {
        return membershipRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Membership.class);
        });
    }

    @Override
    public Optional<Membership> getMembershipByMobile(final String clientId, final String mobile) {
        return membershipRepository.findByClientIdAndMobileNumber(clientId, mobile);
    }
}
