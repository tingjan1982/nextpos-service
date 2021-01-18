package io.nextpos.shared.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTimeUtil {

    public static LocalDateTime toLocalDateTime(ZoneId zoneId, Date date) {
        return date.toInstant().atZone(zoneId).toLocalDateTime();
    }

    public static Date toDate(ZoneId zoneId, LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(zoneId).toInstant());
    }

    public static String formatDateTime(LocalDateTime localDateTime) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        return localDateTime.format(formatter);
    }
}
