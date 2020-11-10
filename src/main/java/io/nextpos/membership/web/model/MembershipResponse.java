package io.nextpos.membership.web.model;

import io.nextpos.membership.data.Membership;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class MembershipResponse {

    private String id;

    private String name;

    private String phoneNumber;

    private LocalDate birthday;

    private Membership.Gender gender;

    public MembershipResponse(Membership membership) {
        id = membership.getId();
        name = membership.getName();
        phoneNumber = membership.getPhoneNumber();
        birthday = membership.getBirthday();
        gender = membership.getGender();
    }
}
