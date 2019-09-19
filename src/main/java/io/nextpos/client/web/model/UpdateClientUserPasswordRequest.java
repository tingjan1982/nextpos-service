package io.nextpos.client.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClientUserPasswordRequest {

    @Size(min = 4, max = 8)
    @Pattern(regexp="^([0-9]*)$")
    private String password;
}
