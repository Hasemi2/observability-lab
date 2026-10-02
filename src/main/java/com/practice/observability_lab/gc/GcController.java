package com.practice.observability_lab.gc;

import com.practice.observability_lab.gc.dto.GcChurnResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("gc")
@RestController
@RequestMapping("/gc")
public class GcController {

    private final GcService gcService;

    public GcController(GcService gcService) {
        this.gcService = gcService;
    }

    @PostMapping("/churn")
    public GcChurnResponse churn(
            @RequestParam(required = false) Long totalMebibytes,
            @RequestParam(required = false) Integer chunkKibibytes
    ) {
        return gcService.churn(totalMebibytes, chunkKibibytes);
    }
}
