package io.nextpos.shared.converter;

import org.springframework.core.convert.converter.Converter;

import java.time.LocalTime;

public class StringToLocalTime implements Converter<String, LocalTime> {

    @Override
    public LocalTime convert(String timeStr) {
        return LocalTime.parse(timeStr);
    }
}
