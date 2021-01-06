package io.nextpos.reporting.web.model;

import io.nextpos.datetime.data.ZonedDateRange;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class BaseDateRangeResponse {

    protected ZonedDateRange dateRange;

    public BaseDateRangeResponse(ZonedDateRange dateRange) {
        this.dateRange = dateRange;
    }
}
