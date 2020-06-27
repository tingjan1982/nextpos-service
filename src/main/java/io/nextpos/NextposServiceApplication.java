package io.nextpos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@SpringBootApplication
public class NextposServiceApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(NextposServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NextposServiceApplication.class, args);
    }

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        LOGGER.info("Configured locale: {}, timezone: {}", Locale.getDefault(), TimeZone.getDefault());
        LOGGER.info("Date in UTC: {}", new Date());
    }
}
