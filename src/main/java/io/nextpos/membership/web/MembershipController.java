package io.nextpos.membership.web;

import io.nextpos.client.data.Client;
import io.nextpos.membership.data.Membership;
import io.nextpos.membership.data.OrderTopRanking;
import io.nextpos.membership.service.MembershipReportService;
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

    private final MembershipReportService membershipReportService;

    @Autowired
    public MembershipController(MembershipService membershipService, MembershipReportService membershipReportService) {
        this.membershipService = membershipService;
        this.membershipReportService = membershipReportService;
    }

    @PostMapping
    public MembershipResponse createMembership(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                               @Valid @RequestBody MembershipRequest request) {

        Membership membership = fromMembershipRequest(client, request);
        membershipService.saveMembership(membership);

        return toResponse(membership);
    }

    private Membership fromMembershipRequest(Client client, MembershipRequest request) {

        final Membership membership = new Membership(client.getId(), request.getName(), request.getPhoneNumber());

        membership.setGender(request.getGender());
        membership.setBirthday(request.getBirthday());
        membership.setTags(request.getTags());

        return membership;
    }

    @GetMapping("/{id}")
    public MembershipResponse getMembership(@PathVariable String id) {

        final Membership membership = membershipService.getMembershipOrThrows(id);
        final MembershipResponse response = toResponse(membership);

        final List<MembershipResponse.RecentOrderResponse> results = membershipReportService.getRecentOrders(membership, 5).stream()
                .map(MembershipResponse.RecentOrderResponse::new)
                .collect(Collectors.toList());
        response.setRecentOrders(results);

        final List<OrderTopRanking> topRankingOrderLineItems = membershipReportService.getTopRankingOrderLineItems(membership, 5);
        response.setTopRankings(topRankingOrderLineItems);

        return response;
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
        membership.setTags(request.getTags());
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
