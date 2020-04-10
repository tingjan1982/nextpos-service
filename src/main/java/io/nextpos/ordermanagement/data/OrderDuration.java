package io.nextpos.ordermanagement.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDuration {

    private Date startDate;

    private Date endDate;

    private long durationHours;

    private long durationMinutes;
}
