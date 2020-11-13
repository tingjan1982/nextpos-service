package io.nextpos.membership.web.model;

import io.nextpos.membership.data.Membership;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class MembershipRequest {

    private String name;

    @NotBlank
    private String phoneNumber;

    private LocalDate birthday;

    private Membership.Gender gender;
}
