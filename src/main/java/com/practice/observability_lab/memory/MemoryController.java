package com.practice.observability_lab.memory;

import com.practice.observability_lab.memory.dto.MemoryAllocationResponse;
import com.practice.observability_lab.memory.dto.MemoryReleaseResponse;
import com.practice.observability_lab.memory.dto.MemoryStatusResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("memory")
@RestController
@RequestMapping("/memory")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @PostMapping("/allocate")
    public MemoryAllocationResponse allocate(
            @RequestParam(defaultValue = "10") long mebibytes
    ) {
        return memoryService.allocate(mebibytes);
    }

    @GetMapping("/status")
    public MemoryStatusResponse status() {
        return memoryService.status();
    }

    @DeleteMapping
    public MemoryReleaseResponse release() {
        return memoryService.release();
    }
}
