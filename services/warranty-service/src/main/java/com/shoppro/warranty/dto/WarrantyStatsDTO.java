package com.shoppro.warranty.dto;

import com.shoppro.warranty.entity.WarrantyStatus;

import java.util.HashMap;
import java.util.Map;

public class WarrantyStatsDTO {

    private final Map<WarrantyStatus, Long> statusCounts = new HashMap<>();

    public void addStatusCount(WarrantyStatus status, Long count) {
        statusCounts.put(status, count);
    }

    public Map<WarrantyStatus, Long> getStatusCounts() {
        return statusCounts;
    }

    public Long getCountByStatus(WarrantyStatus status) {
        return statusCounts.getOrDefault(status, 0L);
    }

    public Long getTotalRequests() {
        return statusCounts.values().stream().mapToLong(Long::longValue).sum();
    }
}
