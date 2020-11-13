package io.nextpos.membership.service;

import io.nextpos.client.data.Client;
import io.nextpos.membership.data.Membership;
import io.nextpos.membership.data.MembershipRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.MongoTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@MongoTransaction
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
    public Optional<Membership> getMembership(String id) {
        return membershipRepository.findById(id);
    }

    @Override
    public Membership getMembershipOrThrows(final String id) {
        return membershipRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Membership.class);
        });
    }

    @Override
    public Optional<Membership> getMembershipByPhoneNumber(final String clientId, final String mobile) {
        return membershipRepository.findByClientIdAndPhoneNumber(clientId, mobile);
    }

    @Override
    public List<Membership> getMemberships(Client client) {
        return membershipRepository.findAllByClientId(client.getId());
    }

    @Override
    public void deleteMembership(Membership membership) {
        membershipRepository.delete(membership);
    }
}
