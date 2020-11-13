package io.nextpos.membership.web;

import io.nextpos.client.data.Client;
import io.nextpos.membership.data.Membership;
import io.nextpos.membership.service.MembershipService;
import io.nextpos.membership.web.model.MembershipRequest;
import io.nextpos.membership.web.model.MembershipResponse;
import io.nextpos.membership.web.model.MembershipsResponse;
import io.nextpos.shared.web.ClientResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/memberships")
public class MembershipController {

    private final MembershipService membershipService;

    @Autowired
    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    public MembershipResponse getOrCreateMembership(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                    @Valid @RequestBody MembershipRequest request) {

        final Membership membership = membershipService.getMembershipByPhoneNumber(client.getId(), request.getPhoneNumber()).orElseGet(() -> {
            Membership newMembership = fromMembershipRequest(client, request);
            return membershipService.saveMembership(newMembership);
        });

        return toResponse(membership);
    }

    private Membership fromMembershipRequest(Client client, MembershipRequest request) {

        return new Membership(client.getId(), request.getName(), request.getPhoneNumber());
    }

    @GetMapping("/{id}")
    public MembershipResponse getMembership(@PathVariable String id) {

        return toResponse(membershipService.getMembershipOrThrows(id));
    }

    @GetMapping
    public MembershipsResponse getMemberships(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                              @RequestParam(value = "phoneNumber", required = false) String phoneNumber) {

        if (StringUtils.isNotBlank(phoneNumber)) {
            final List<MembershipResponse> results = membershipService.getMembershipByPhoneNumber(client.getId(), phoneNumber).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());

            return new MembershipsResponse(results);
        } else {
            final List<MembershipResponse> results = membershipService.getMemberships(client).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());

            return new MembershipsResponse(results);
        }
    }

    @PostMapping("/{id}")
    public MembershipResponse updateMembership(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                               @PathVariable String id,
                                               @Valid @RequestBody MembershipRequest request) {

        final Membership membership = membershipService.getMembershipOrThrows(id);
        updateFromMembershipRequest(membership, request);

        return toResponse(membershipService.saveMembership(membership));
    }

    private void updateFromMembershipRequest(Membership membership, MembershipRequest request) {

        membership.setName(request.getName());
        membership.setBirthday(request.getBirthday());
        membership.setGender(request.getGender());
    }

    private MembershipResponse toResponse(Membership membership) {
        return new MembershipResponse(membership);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMembership(@PathVariable String id) {

        final Membership membership = membershipService.getMembershipOrThrows(id);
        membershipService.deleteMembership(membership);
    }
}
