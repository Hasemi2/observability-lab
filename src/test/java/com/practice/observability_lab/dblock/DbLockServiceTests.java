package com.practice.observability_lab.dblock;

import com.practice.observability_lab.dblock.dto.DbLockResponse;
import com.practice.observability_lab.dblock.exception.InvalidDbLockRequestException;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.profiles.active=db-lock")
class DbLockServiceTests {

    @Autowired
    private DbLockService dbLockService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void updateWaitsUntilTheHoldingTransactionReleasesTheRowLock() throws Exception {
        CompletableFuture<DbLockResponse> holder = CompletableFuture.supplyAsync(() -> {
            try {
                return dbLockService.hold(1);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        });

        awaitHolder();
        CompletableFuture<DbLockResponse> updater = CompletableFuture.supplyAsync(dbLockService::update);

        Thread.sleep(200);
        assertThat(updater).isNotDone();

        holder.get(3, TimeUnit.SECONDS);
        DbLockResponse updateResponse = updater.get(3, TimeUnit.SECONDS);

        assertThat(updateResponse.operation()).isEqualTo("update");
        assertThat(updateResponse.waitedMillis()).isGreaterThanOrEqualTo(500);
    }

    @Test
    void holdRejectsSecondsOutsideTheConfiguredRange() {
        assertThatThrownBy(() -> dbLockService.hold(31))
                .isInstanceOf(InvalidDbLockRequestException.class)
                .hasMessageContaining("between 1 and 30");
    }

    private void awaitHolder() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (meterRegistry.get("lab.db.lock.holders").gauge().value() == 1) {
                return;
            }
            Thread.onSpinWait();
        }
        throw new AssertionError("the holding transaction did not acquire the row lock");
    }
}
