package io.nextpos.roster.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class RosterResourceRequest {

    private List<String> workingAreas = new ArrayList<>();
}
