package io.nextpos.roster.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RosterPlansResponse {

    private List<RosterPlanResponse> results;
}
