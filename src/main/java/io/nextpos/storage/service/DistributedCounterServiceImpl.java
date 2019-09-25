package io.nextpos.storage.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reference
 *
 * Spring Boot with Hazelcast
 * https://medium.com/@ihorkosandiak/spring-boot-with-hazelcast-b04d13927745
 *
 * Hazelcast Client:
 * https://www.baeldung.com/java-hazelcast
 *
 * Hazelcast sidecar pattern:
 * https://hazelcast.com/blog/hazelcast-sidecar-container-pattern/
 */
@Service
public class DistributedCounterServiceImpl implements DistributedCounterService {

    private static final String COUNTER = "counter";

    static final int MAX_COUNTER = 500;
    
    private final HazelcastInstance hazelcastInstance;

    @Autowired
    public DistributedCounterServiceImpl(final HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public int getNextRotatingCounter(final String counterKey) {

        final IMap<String, AtomicInteger> counterMap = hazelcastInstance.getMap(COUNTER);
        final AtomicInteger counter = counterMap.computeIfAbsent(counterKey, (k) -> new AtomicInteger(0));
        final int nextCounter = counter.incrementAndGet();
        counter.compareAndSet(MAX_COUNTER, 0);
        counterMap.put(counterKey, counter);
        
        return nextCounter;
    }

    @Override
    public int getCurrentCounter(final String counterKey) {

        final IMap<String, AtomicInteger> counterMap = hazelcastInstance.getMap(COUNTER);
        final AtomicInteger counter = counterMap.get(counterKey);

        return counter == null ? 0 : counter.get();
    }
}
