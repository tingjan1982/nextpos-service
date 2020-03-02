package io.nextpos.ordermanagement.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDuration {

    private Date orderSubmittedDate;

    private Date orderSettledDate;

    private long durationHours;

    private long durationMinutes;
}
