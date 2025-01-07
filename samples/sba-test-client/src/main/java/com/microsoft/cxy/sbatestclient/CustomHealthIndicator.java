package com.microsoft.cxy.sbatestclient;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator implements HealthIndicator {
    private SbaTestClientApplication client = new SbaTestClientApplication();
    @Override
    public Health health() {
        // Perform custom health check logic (e.g., ping a service or check a dependency)
        boolean dbIsHealthy = checkDatabase();
        if (dbIsHealthy) {
            return Health.up().withDetail("Database Status", "Available").build();
        }
        return Health.down().withDetail("Database Status", "Unavailable").build();
    }

    private boolean checkDatabase() {
        // Custom database check logic
        long startTime = System.currentTimeMillis(); // Start timer

        // Perform the database check logic (e.g., ping a database or run a test query)
        client.accessDB(true);

        long endTime = System.currentTimeMillis();   // End timer
        long executionTime = endTime - startTime;    // Calculate execution time in milliseconds

        return executionTime < 1000;
    }
}