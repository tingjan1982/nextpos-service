package io.nextpos.workingarea.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WorkingAreasResponse {

    private List<WorkingAreaResponse> workingAreas;
}
