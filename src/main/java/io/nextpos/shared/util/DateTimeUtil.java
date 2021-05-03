package io.nextpos.shared.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTimeUtil {

    public static LocalDate toLocalDate(ZoneId zoneId, Date date) {
        return date.toInstant().atZone(zoneId).toLocalDate();
    }

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

    public static String formatDate(ZoneId zoneId, Date date) {
        return formatDateTime(toLocalDateTime(zoneId, date));
    }

    public static boolean dateEquals(LocalDateTime date1, LocalDateTime date2) {
        return date1 != null && date2 != null && date1.compareTo(date2) == 0;
    }
}
