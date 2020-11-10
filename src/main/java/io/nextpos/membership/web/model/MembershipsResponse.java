package io.nextpos.membership.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MembershipsResponse {

    private List<MembershipResponse> results;
}
