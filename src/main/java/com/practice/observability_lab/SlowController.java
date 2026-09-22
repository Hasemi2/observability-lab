package com.practice.observability_lab;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SlowController {

    @GetMapping("/slow")
    public String slow(@RequestParam(required = false, defaultValue = "0") long seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000L);
        return "slow";
    }
}
