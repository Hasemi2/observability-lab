package com.practice.observability_lab.externalapi;

import com.practice.observability_lab.externalapi.dto.ExternalCallResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("external-api")
@RestController
@RequestMapping("/external")
public class ExternalApiController {

    private final ExternalApiService externalApiService;

    public ExternalApiController(ExternalApiService externalApiService) {
        this.externalApiService = externalApiService;
    }

    @GetMapping("/normal")
    public ExternalCallResponse normal() {
        return externalApiService.callNormal();
    }

    @GetMapping("/delay")
    public ExternalCallResponse delay(
            @RequestParam(defaultValue = "1000") long milliseconds
    ) {
        return externalApiService.callDelay(milliseconds);
    }

    @GetMapping("/error")
    public ExternalCallResponse error(
            @RequestParam(defaultValue = "500") int status
    ) {
        return externalApiService.callError(status);
    }
}
