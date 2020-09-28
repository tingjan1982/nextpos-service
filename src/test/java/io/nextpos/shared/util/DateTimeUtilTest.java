package io.nextpos.shared.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

class DateTimeUtilTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateTimeUtilTest.class);

    @Test
    void toLocalDateTime() {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        LOGGER.info("{}", ZoneId.systemDefault());

        LOGGER.info("{}", DateTimeUtil.toLocalDateTime(ZoneId.systemDefault(), new Date()));
        LOGGER.info("{}", DateTimeUtil.toLocalDateTime(ZoneId.of("Asia/Taipei"), new Date()));
    }
}