package io.nextpos.shared.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class DateTimeUtil {

    public static LocalDateTime toLocalDateTime(ZoneId zoneId, Date date) {
        return date.toInstant().atZone(zoneId).toLocalDateTime();
    }
}