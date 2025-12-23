package com.example.admin.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Component
public class DatabaseMonitor {

    private static final String STATUS_KEY = "status";
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String DATABASE_TYPE = "MySQL";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String ERROR_KEY = "error";
    private static final int HEALTH_CHECK_TIMEOUT_SECONDS = 5;

    @Autowired
    private DataSource dataSource;

    /**
     * Get connection pool status
     */
    public Map<String, Object> getConnectionPoolStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                
                status.put(STATUS_KEY, STATUS_UP);
                status.put("database", DATABASE_TYPE);
                status.put("poolName", hikariDataSource.getPoolName());
                status.put("totalConnections", poolMXBean.getTotalConnections());
                status.put("activeConnections", poolMXBean.getActiveConnections());
                status.put("idleConnections", poolMXBean.getIdleConnections());
                status.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
                status.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
                status.put("minimumIdle", hikariDataSource.getMinimumIdle());
                status.put("connectionTimeout", hikariDataSource.getConnectionTimeout());
                status.put(TIMESTAMP_KEY, System.currentTimeMillis());
            } else {
                status.put(STATUS_KEY, STATUS_UP);
                status.put("database", DATABASE_TYPE);
                status.put("poolType", dataSource.getClass().getSimpleName());
                status.put("message", "Basic connection pool - detailed metrics not available");
                status.put(TIMESTAMP_KEY, System.currentTimeMillis());
            }
        } catch (Exception e) {
            status.put(STATUS_KEY, STATUS_DOWN);
            status.put(ERROR_KEY, e.getMessage());
            status.put(TIMESTAMP_KEY, System.currentTimeMillis());
        }
        
        return status;
    }

    /**
     * Check if database is healthy
     * 
     * @return true if database connection is valid, false otherwise
     */
    public boolean isDatabaseHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(HEALTH_CHECK_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Get database health details
     */
    public Map<String, Object> getDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(HEALTH_CHECK_TIMEOUT_SECONDS);
            
            health.put(STATUS_KEY, isValid ? STATUS_UP : STATUS_DOWN);
            health.put("database", DATABASE_TYPE);
            health.put("url", connection.getMetaData().getURL());
            health.put("databaseProductName", connection.getMetaData().getDatabaseProductName());
            health.put("databaseProductVersion", connection.getMetaData().getDatabaseProductVersion());
            health.put("driverName", connection.getMetaData().getDriverName());
            health.put("driverVersion", connection.getMetaData().getDriverVersion());
            health.put("isReadOnly", connection.isReadOnly());
            health.put("transactionIsolation", getTransactionIsolationName(connection.getTransactionIsolation()));
            health.put(TIMESTAMP_KEY, System.currentTimeMillis());
            
        } catch (SQLException e) {
            health.put(STATUS_KEY, STATUS_DOWN);
            health.put(ERROR_KEY, e.getMessage());
            health.put(TIMESTAMP_KEY, System.currentTimeMillis());
        }
        
        return health;
    }

    /**
     * Get detailed database metrics
     */
    public Map<String, Object> getDatabaseMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Connection pool metrics
            Map<String, Object> poolStatus = getConnectionPoolStatus();
            metrics.put("connectionPool", poolStatus);
            
            // Database health
            Map<String, Object> dbHealth = getDatabaseHealth();
            metrics.put("health", dbHealth);
            
            // Overall status
            boolean isHealthy = isDatabaseHealthy();
            metrics.put("overallStatus", isHealthy ? STATUS_UP : STATUS_DOWN);
            metrics.put(TIMESTAMP_KEY, System.currentTimeMillis());
            
        } catch (Exception e) {
            metrics.put(STATUS_KEY, "ERROR");
            metrics.put(ERROR_KEY, e.getMessage());
            metrics.put(TIMESTAMP_KEY, System.currentTimeMillis());
        }
        
        return metrics;
    }

    /**
     * Get transaction isolation level name
     */
    private String getTransactionIsolationName(int level) {
        return switch (level) {
            case Connection.TRANSACTION_NONE -> "NONE";
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED -> "READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE -> "SERIALIZABLE";
            default -> "UNKNOWN";
        };
    }
}

