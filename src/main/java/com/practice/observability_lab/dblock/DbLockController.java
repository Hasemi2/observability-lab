package com.practice.observability_lab.dblock;

import com.practice.observability_lab.dblock.dto.DbLockResponse;
import com.practice.observability_lab.dblock.dto.DbLockStatusResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("db-lock")
@RestController
@RequestMapping("/db/lock")
public class DbLockController {

    private final DbLockService dbLockService;

    public DbLockController(DbLockService dbLockService) {
        this.dbLockService = dbLockService;
    }

    @PostMapping("/hold")
    public DbLockResponse hold(
            @RequestParam(defaultValue = "10") long seconds
    ) throws InterruptedException {
        return dbLockService.hold(seconds);
    }

    @PostMapping("/update")
    public DbLockResponse update() {
        return dbLockService.update();
    }

    @GetMapping("/status")
    public DbLockStatusResponse status() {
        return dbLockService.status();
    }
}

