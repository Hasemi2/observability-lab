package com.practice.observability_lab.connectionpool;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/db")
public class ConnectionPoolController {

    private static final long MAX_HOLD_SECONDS = 30;

    private final ConnectionPoolService connectionPoolService;

    public ConnectionPoolController(ConnectionPoolService connectionPoolService) {
        this.connectionPoolService = connectionPoolService;
    }

    @GetMapping("/hold")
    public ConnectionHoldResponse hold(
            @RequestParam(defaultValue = "5") long seconds
    ) throws InterruptedException {
        if (seconds < 1 || seconds > MAX_HOLD_SECONDS) {
            throw new IllegalArgumentException(
                    "seconds must be between 1 and " + MAX_HOLD_SECONDS
            );
        }

        connectionPoolService.holdConnection(seconds);

        return new ConnectionHoldResponse(
                "DB connection released",
                seconds,
                Thread.currentThread().getName()
        );
    }

    public record ConnectionHoldResponse(
            String message,
            long heldSeconds,
            String threadName
    ) {
    }
}
