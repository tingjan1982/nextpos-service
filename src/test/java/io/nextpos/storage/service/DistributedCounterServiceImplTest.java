package io.nextpos.storage.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DistributedCounterServiceImplTest {

    @Autowired
    private DistributedCounterService distributedCounterService;


    @Test
    void rotateCounter() {

        final String counterKey = DistributedCounterService.class.getName();

        assertThat(distributedCounterService.getCurrentCounter(counterKey)).isEqualTo(0);

        for (int i = 1; i <= DistributedCounterServiceImpl.MAX_COUNTER; i++) {
            assertThat(distributedCounterService.getNextRotatingCounter(counterKey)).isEqualTo(i);
        }

        assertThat(distributedCounterService.getNextRotatingCounter(counterKey)).isEqualTo(1);
        assertThat(distributedCounterService.getCurrentCounter(counterKey)).isEqualTo(1);
    }
}