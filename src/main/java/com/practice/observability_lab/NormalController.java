package com.practice.observability_lab;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NormalController {

    @GetMapping("/normal")
    public String normal() {
        return "normal";
    }
}
