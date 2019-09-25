package io.nextpos.storage.web;

import io.nextpos.storage.service.DistributedCounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/counters")
public class CounterController {

    private final DistributedCounterService distributedCounterService;

    @Autowired
    public CounterController(final DistributedCounterService distributedCounterService) {
        this.distributedCounterService = distributedCounterService;
    }

    @GetMapping("/{counterKey}/next")
    public int getNextCounter(@PathVariable final String counterKey) {
        return distributedCounterService.getNextRotatingCounter(counterKey);
    }

    @GetMapping("/{counterKey}")
    public int getCurrentCounter(@PathVariable final String counterKey) {
        return distributedCounterService.getCurrentCounter(counterKey);
    }
}
