package com.practice.observability_lab.connectionpool;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectionPoolService {

    private final EntityManager entityManager;

    public ConnectionPoolService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void holdConnection(long seconds) throws InterruptedException {
        entityManager.createNativeQuery("SELECT 1").getSingleResult();
        Thread.sleep(seconds * 1000L);
    }
}
