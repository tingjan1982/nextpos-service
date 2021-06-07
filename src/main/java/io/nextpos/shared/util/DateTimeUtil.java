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
        return formatDateTime(localDateTime, "yyyy/MM/dd HH:mm:ss");
    }

    public static String formatDateTime(LocalDateTime localDateTime, String pattern) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return localDateTime.format(formatter);
    }

    public static String formatDate(ZoneId zoneId, Date date) {
        return formatDateTime(toLocalDateTime(zoneId, date));
    }

    public static String formatDate(ZoneId zoneId, Date date, String pattern) {
        return formatDateTime(toLocalDateTime(zoneId, date), pattern);
    }
}
