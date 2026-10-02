package com.practice.observability_lab.gc.exception;

public class GcChurnAlreadyRunningException extends RuntimeException {

    public GcChurnAlreadyRunningException() {
        super("a GC churn experiment is already running");
    }
}
