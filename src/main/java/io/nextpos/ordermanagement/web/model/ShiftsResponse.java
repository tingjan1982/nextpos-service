package io.nextpos.ordermanagement.web.model;

import io.nextpos.datetime.data.ZonedDateRange;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ShiftsResponse {

    private ZonedDateRange dateRange;

    private List<ShiftResponse> shifts;
}
