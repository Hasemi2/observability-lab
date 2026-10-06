package com.practice.observability_lab.dblock;

import com.practice.observability_lab.dblock.dto.DbLockResponse;
import com.practice.observability_lab.dblock.dto.DbLockStatusResponse;
import com.practice.observability_lab.dblock.exception.DbLockAcquisitionException;
import com.practice.observability_lab.dblock.exception.InvalidDbLockRequestException;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Profile("db-lock")
@Service
public class DbLockService {

    private static final long TARGET_ID = 1L;

    private final JdbcTemplate jdbcTemplate;
    private final DbLockProperties properties;
    private final AtomicInteger activeHolders = new AtomicInteger();
    private final AtomicInteger waitingRequests = new AtomicInteger();
    private final Timer holdAcquireTimer;
    private final Timer updateAcquireTimer;

    public DbLockService(
            JdbcTemplate jdbcTemplate,
            DbLockProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        validateProperties();

        Gauge.builder("lab.db.lock.holders", activeHolders, AtomicInteger::get)
                .description("Requests currently holding the lab database row lock")
                .register(meterRegistry);
        Gauge.builder("lab.db.lock.waiting", waitingRequests, AtomicInteger::get)
                .description("Requests currently waiting to acquire the lab database row lock")
                .register(meterRegistry);

        holdAcquireTimer = Timer.builder("lab.db.lock.acquire")
                .tag("operation", "hold")
                .description("Time spent acquiring the lab database row lock")
                .register(meterRegistry);
        updateAcquireTimer = Timer.builder("lab.db.lock.acquire")
                .tag("operation", "update")
                .description("Time spent acquiring the lab database row lock")
                .register(meterRegistry);
    }

    @Transactional
    public DbLockResponse hold(long seconds) throws InterruptedException {
        validateHoldSeconds(seconds);

        LockAcquisition acquisition = acquireLock(holdAcquireTimer);
        activeHolders.incrementAndGet();

        try {
            Thread.sleep(Math.multiplyExact(seconds, 1000L));
            long updatedValue = acquisition.value() + 1;
            updateValue(updatedValue);

            return response("hold", acquisition.waitedMillis(), seconds * 1000L, updatedValue);
        } finally {
            activeHolders.decrementAndGet();
        }
    }

    @Transactional
    public DbLockResponse update() {
        LockAcquisition acquisition = acquireLock(updateAcquireTimer);
        activeHolders.incrementAndGet();

        try {
            long updatedValue = acquisition.value() + 1;
            updateValue(updatedValue);
            return response("update", acquisition.waitedMillis(), 0, updatedValue);
        } finally {
            activeHolders.decrementAndGet();
        }
    }

    @Transactional(readOnly = true)
    public DbLockStatusResponse status() {
        Long value = jdbcTemplate.queryForObject(
                "SELECT lock_value FROM db_lock_target WHERE id = ?",
                Long.class,
                TARGET_ID
        );

        return new DbLockStatusResponse(
                value == null ? 0 : value,
                activeHolders.get(),
                waitingRequests.get()
        );
    }

    private LockAcquisition acquireLock(Timer timer) {
        waitingRequests.incrementAndGet();
        long startedAt = System.nanoTime();

        try {
            Long value = jdbcTemplate.queryForObject(
                    "SELECT lock_value FROM db_lock_target WHERE id = ? FOR UPDATE",
                    Long.class,
                    TARGET_ID
            );
            long waitedNanos = System.nanoTime() - startedAt;
            timer.record(waitedNanos, TimeUnit.NANOSECONDS);
            return new LockAcquisition(
                    value == null ? 0 : value,
                    TimeUnit.NANOSECONDS.toMillis(waitedNanos)
            );
        } catch (DataAccessException exception) {
            throw new DbLockAcquisitionException(exception);
        } finally {
            waitingRequests.decrementAndGet();
        }
    }

    private void updateValue(long value) {
        jdbcTemplate.update(
                "UPDATE db_lock_target SET lock_value = ? WHERE id = ?",
                value,
                TARGET_ID
        );
    }

    private DbLockResponse response(
            String operation,
            long waitedMillis,
            long heldMillis,
            long value
    ) {
        return new DbLockResponse(
                operation,
                waitedMillis,
                heldMillis,
                value,
                Thread.currentThread().getName()
        );
    }

    private void validateHoldSeconds(long seconds) {
        if (seconds < 1 || seconds > properties.getMaxHoldSeconds()) {
            throw new InvalidDbLockRequestException(
                    "seconds must be between 1 and " + properties.getMaxHoldSeconds()
            );
        }
    }

    private void validateProperties() {
        if (properties.getMaxHoldSeconds() < 1) {
            throw new IllegalStateException("lab.db-lock.max-hold-seconds must be at least 1");
        }
    }

    private record LockAcquisition(long value, long waitedMillis) {
    }
}
