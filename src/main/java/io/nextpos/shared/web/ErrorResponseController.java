package io.nextpos.shared.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A test API to test various error response scenarios.
 *
 * https://reflectoring.io/spring-boot-exception-handling/
 */
@RestController
@RequestMapping("/errortest")
public class ErrorResponseController {

    /**
     * Use trace=true in the request to see stacktrace.
     */
    @GetMapping("/runtime")
    public void unexpectedException() {

        throw new RuntimeException("test message");
    }
}
