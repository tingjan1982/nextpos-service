package io.nextpos.roster.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
public class RosterUserRequest {

    @NotNull
    private List<String> usernames;
}
