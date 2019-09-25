package io.nextpos.storage.service;

public interface DistributedCounterService {

    int getNextRotatingCounter(String counterKey);

    int getCurrentCounter(String counterKey);
}
