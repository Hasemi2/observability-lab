package com.practice.observability_lab.mockexternal;

import com.practice.observability_lab.mockexternal.dto.MockExternalResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("external-api")
@RestController
@RequestMapping("/mock/external")
public class MockExternalController {

    private final MockExternalService mockExternalService;

    public MockExternalController(MockExternalService mockExternalService) {
        this.mockExternalService = mockExternalService;
    }

    @GetMapping("/normal")
    public MockExternalResponse normal() {
        return mockExternalService.normal();
    }

    @GetMapping("/delay")
    public MockExternalResponse delay(
            @RequestParam(defaultValue = "5000") long milliseconds
    ) throws InterruptedException {
        return mockExternalService.delay(milliseconds);
    }

    @GetMapping("/error")
    public void error(
            @RequestParam(defaultValue = "500") int status
    ) {
        mockExternalService.error(status);
    }
}

