package io.nextpos.roster.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class RosterResourceRequest {

    private Map<String, List<String>> workingAreaToUsernames;
}
