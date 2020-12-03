package io.nextpos.ordermanagement.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class OrderMembershipRequest {

    @NotBlank
    private String membershipId;
}
