package io.nextpos.script;

import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.workingarea.data.PrinterRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class UpdatePrinterServiceTypes {

    static {
        System.setProperty("jdk.tls.client.protocols", "TLSv1, TLSv1.1, TLSv1.2");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePrinterServiceTypes.class);

    private final PrinterRepository printerRepository;

    @Autowired
    public UpdatePrinterServiceTypes(PrinterRepository printerRepository) {
        this.printerRepository = printerRepository;
    }


    @Test
    @Rollback(value = false)
    @ChainedTransaction
    void update() {
        AtomicInteger count = new AtomicInteger();

        printerRepository.findAll().forEach(p -> {
            count.incrementAndGet();
            LOGGER.info("Found printer {}", p.getName());
            p.setServiceTypes(Set.of(p.getServiceType()));
            LOGGER.info("Check printer service type: {}", p.getServiceType() == p.getServiceTypes().iterator().next());

            // it seems like if record is modified within a transaction, changes will be flushed and persisted without explicit save.
            //printerRepository.save(p);
        });

        LOGGER.info("Processed {} printers.", count.get());
    }
}
