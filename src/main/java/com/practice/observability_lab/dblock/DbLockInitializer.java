package com.practice.observability_lab.dblock;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Profile("db-lock")
@Component
public class DbLockInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DbLockInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS db_lock_target (
                    id BIGINT PRIMARY KEY,
                    lock_value BIGINT NOT NULL
                )
                """);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM db_lock_target WHERE id = ?",
                Integer.class,
                1L
        );

        if (count != null && count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO db_lock_target (id, lock_value) VALUES (?, ?)",
                    1L,
                    0L
            );
        }
    }
}
