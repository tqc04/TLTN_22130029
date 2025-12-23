package com.example.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AnalyticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    
    @Value("${services.order.base-url:http://localhost:8084}")
    private String orderServiceUrl;
    
    @Value("${services.user.base-url:http://localhost:8082}")
    private String userServiceUrl;
    
    @Value("${services.product.base-url:http://localhost:8083}")
    private String productServiceUrl;
    
    @Value("${services.inventory.base-url:http://localhost:8093}")
    private String inventoryServiceUrl;
    
    @Value("${interservice.username:service}")
    private String interServiceUsername;
    
    @Value("${interservice.password:service123}")
    private String interServicePassword;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Get overview statistics
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        try {
            // Get total sales from orders
            BigDecimal totalSales = getTotalSales();
            overview.put("totalSales", totalSales != null ? totalSales.doubleValue() : 0.0);
            
            // Get total orders count
            Long totalOrders = getTotalOrdersCount();
            overview.put("totalOrders", totalOrders != null ? totalOrders : 0L);
            
            // Get active users count
            Long activeUsers = getActiveUsersCount();
            overview.put("activeUsers", activeUsers != null ? activeUsers : 0L);
            
            // Get today's sales
            BigDecimal todaySales = getSalesByDateRange(LocalDate.now(), LocalDate.now());
            overview.put("todaySales", todaySales != null ? todaySales.doubleValue() : 0.0);
            
            // Get today's orders
            Long todayOrders = getOrdersCountByDateRange(LocalDate.now(), LocalDate.now());
            overview.put("todayOrders", todayOrders != null ? todayOrders : 0L);
            
            overview.put("date", LocalDate.now().toString());
            
        } catch (Exception e) {
            logger.error("Error getting overview statistics: {}", e.getMessage(), e);
            overview.put("totalSales", 0.0);
            overview.put("totalOrders", 0L);
            overview.put("activeUsers", 0L);
            overview.put("todaySales", 0.0);
            overview.put("todayOrders", 0L);
            overview.put("date", LocalDate.now().toString());
        }
        
        return overview;
    }
    
    /**
     * Get sales data by time range
     */
    public Map<String, Object> getSalesByTimeRange(String timeRange) {
        Map<String, Object> result = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate;
            
            switch (timeRange.toLowerCase()) {
                case "7days":
                    startDate = endDate.minusDays(6);
                    break;
                case "30days":
                    startDate = endDate.minusDays(29);
                    break;
                case "90days":
                    startDate = endDate.minusDays(89);
                    break;
                case "year":
                    startDate = endDate.minusYears(1);
                    break;
                default:
                    startDate = endDate.minusDays(29); // Default to 30 days
            }
            
            // Get daily sales data
            LocalDate currentDate = startDate;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
            
            while (!currentDate.isAfter(endDate)) {
                labels.add(currentDate.format(formatter));
                BigDecimal daySales = getSalesByDateRange(currentDate, currentDate);
                values.add(daySales != null ? daySales.doubleValue() : 0.0);
                currentDate = currentDate.plusDays(1);
            }
            
            result.put("labels", labels);
            result.put("values", values);
            
        } catch (Exception e) {
            logger.error("Error getting sales by time range: {}", e.getMessage(), e);
            result.put("labels", Collections.emptyList());
            result.put("values", Collections.emptyList());
        }
        
        return result;
    }
    
    /**
     * Get sales statistics by period (day, week, month, year)
     */
    public Map<String, Object> getSalesByPeriod(String period) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();
        
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate;
            
            switch (period.toLowerCase()) {
                case "day":
                    // Last 30 days
                    startDate = endDate.minusDays(29);
                    LocalDate currentDate = startDate;
                    while (!currentDate.isAfter(endDate)) {
                        Map<String, Object> dayData = new HashMap<>();
                        dayData.put("date", currentDate.toString());
                        dayData.put("label", currentDate.format(DateTimeFormatter.ofPattern("MM/dd")));
                        BigDecimal sales = getSalesByDateRange(currentDate, currentDate);
                        dayData.put("sales", sales != null ? sales.doubleValue() : 0.0);
                        Long orders = getOrdersCountByDateRange(currentDate, currentDate);
                        dayData.put("orders", orders != null ? orders : 0L);
                        data.add(dayData);
                        currentDate = currentDate.plusDays(1);
                    }
                    break;
                    
                case "week":
                    // Last 12 weeks
                    for (int i = 11; i >= 0; i--) {
                        LocalDate weekStart = endDate.minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
                        LocalDate weekEnd = weekStart.plusDays(6);
                        Map<String, Object> weekData = new HashMap<>();
                        weekData.put("date", weekStart.toString());
                        weekData.put("label", "Week " + (12 - i));
                        BigDecimal sales = getSalesByDateRange(weekStart, weekEnd);
                        weekData.put("sales", sales != null ? sales.doubleValue() : 0.0);
                        Long orders = getOrdersCountByDateRange(weekStart, weekEnd);
                        weekData.put("orders", orders != null ? orders : 0L);
                        data.add(weekData);
                    }
                    break;
                    
                case "month":
                    // Last 12 months
                    for (int i = 11; i >= 0; i--) {
                        LocalDate monthStart = endDate.minusMonths(i).withDayOfMonth(1);
                        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
                        Map<String, Object> monthData = new HashMap<>();
                        monthData.put("date", monthStart.toString());
                        monthData.put("label", monthStart.format(DateTimeFormatter.ofPattern("MM/yyyy")));
                        BigDecimal sales = getSalesByDateRange(monthStart, monthEnd);
                        monthData.put("sales", sales != null ? sales.doubleValue() : 0.0);
                        Long orders = getOrdersCountByDateRange(monthStart, monthEnd);
                        monthData.put("orders", orders != null ? orders : 0L);
                        data.add(monthData);
                    }
                    break;
                    
                case "year":
                    // Last 5 years
                    for (int i = 4; i >= 0; i--) {
                        LocalDate yearStart = endDate.minusYears(i).withDayOfYear(1);
                        LocalDate yearEnd = yearStart.plusYears(1).minusDays(1);
                        Map<String, Object> yearData = new HashMap<>();
                        yearData.put("date", yearStart.toString());
                        yearData.put("label", String.valueOf(yearStart.getYear()));
                        BigDecimal sales = getSalesByDateRange(yearStart, yearEnd);
                        yearData.put("sales", sales != null ? sales.doubleValue() : 0.0);
                        Long orders = getOrdersCountByDateRange(yearStart, yearEnd);
                        yearData.put("orders", orders != null ? orders : 0L);
                        data.add(yearData);
                    }
                    break;
            }
            
            result.put("period", period);
            result.put("data", data);
            
        } catch (Exception e) {
            logger.error("Error getting sales by period: {}", e.getMessage(), e);
            result.put("period", period);
            result.put("data", Collections.emptyList());
        }
        
        return result;
    }
    
    /**
     * Get top selling products
     */
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        try {
            // Call order service to get top selling products
            String url = orderServiceUrl + "/api/orders/top-products?limit=" + limit;
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> body = response.getBody();
                if (body != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> topProducts = (List<Map<String, Object>>) body.get("products");
                    
                    if (topProducts != null) {
                        // Return products with their existing data (already enriched from order service)
                        return topProducts;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting top selling products: {}", e.getMessage(), e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Get customer analysis
     */
    public Map<String, Object> getCustomerAnalysis() {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            // Get new users count (last 30 days)
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            Long newUsers = getNewUsersCount(thirtyDaysAgo);
            analysis.put("newUsers", newUsers != null ? newUsers : 0L);
            
            // Get total users
            Long totalUsers = getTotalUsersCount();
            analysis.put("totalUsers", totalUsers != null ? totalUsers : 0L);
            
            // Get retained users (users who made orders in last 30 days)
            Long retainedUsers = getRetainedUsersCount(thirtyDaysAgo);
            analysis.put("retainedUsers", retainedUsers != null ? retainedUsers : 0L);
            
        } catch (Exception e) {
            logger.error("Error getting customer analysis: {}", e.getMessage(), e);
            analysis.put("newUsers", 0L);
            analysis.put("totalUsers", 0L);
            analysis.put("retainedUsers", 0L);
        }
        
        return analysis;
    }
    
    /**
     * Get product statistics (total count)
     */
    public Long getTotalProductsCount() {
        try {
            String url = productServiceUrl + "/api/products?size=1";
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> body = response.getBody();
                if (body != null) {
                    Object totalElements = body.get("totalElements");
                    if (totalElements instanceof Number) {
                        return ((Number) totalElements).longValue();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting total products count: {}", e.getMessage(), e);
        }
        return 0L;
    }
    
    /**
     * Get low stock products count from inventory service
     */
    public Long getLowStockCount() {
        try {
            String url = inventoryServiceUrl + "/api/inventory/low-stock/count";
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> body = response.getBody();
                if (body != null) {
                    Object count = body.get("count");
                    if (count instanceof Number) {
                        return ((Number) count).longValue();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error getting low stock count (inventory service may not have this endpoint): {}", e.getMessage());
            // Fallback: return 0
        }
        return 0L;
    }
    
    /**
     * Get category distribution (sales by category)
     */
    public List<Map<String, Object>> getCategoryDistribution() {
        try {
            String url = orderServiceUrl + "/api/orders/category-distribution";
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> distribution = (List<Map<String, Object>>) response.getBody();
                return distribution;
            }
        } catch (Exception e) {
            logger.warn("Error getting category distribution: {}", e.getMessage());
        }
        return new ArrayList<>();
    }
    
    // Helper methods
    
    private BigDecimal getTotalSales() {
        try {
            // Get all completed orders
            String url = orderServiceUrl + "/api/orders?status=COMPLETED&size=10000";
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                Map<?, ?> body = response.getBody();
                if (body != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
                    
                    if (content != null) {
                        return content.stream()
                            .map(order -> {
                                Object totalAmount = order.get("totalAmount");
                                if (totalAmount != null) {
                                    return new BigDecimal(totalAmount.toString());
                                }
                                return BigDecimal.ZERO;
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting total sales: {}", e.getMessage(), e);
        }
        return BigDecimal.ZERO;
    }
    
    private BigDecimal getSalesByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            
            // Get all orders in date range
            String url = orderServiceUrl + "/api/orders?status=COMPLETED&size=10000";
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                Map<?, ?> body = response.getBody();
                if (body != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
                    
                    if (content != null) {
                        return content.stream()
                            .filter(order -> {
                                Object createdAt = order.get("createdAt");
                                if (createdAt != null) {
                                    try {
                                        LocalDateTime orderDate = LocalDateTime.parse(createdAt.toString());
                                        return !orderDate.isBefore(startDateTime) && !orderDate.isAfter(endDateTime);
                                    } catch (Exception e) {
                                        return false;
                                    }
                                }
                                return false;
                            })
                            .map(order -> {
                                Object totalAmount = order.get("totalAmount");
                                if (totalAmount != null) {
                                    return new BigDecimal(totalAmount.toString());
                                }
                                return BigDecimal.ZERO;
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting sales by date range: {}", e.getMessage(), e);
        }
        return BigDecimal.ZERO;
    }
    
    private Long getTotalOrdersCount() {
        try {
            String url = orderServiceUrl + "/api/orders/stats";
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                Map<?, ?> stats = response.getBody();
                if (stats != null) {
                    long total = 0;
                    for (Object value : stats.values()) {
                        if (value instanceof Number) {
                            total += ((Number) value).longValue();
                        }
                    }
                    return total;
                }
            }
        } catch (Exception e) {
            logger.error("Error getting total orders count: {}", e.getMessage(), e);
        }
        return 0L;
    }
    
    private Long getOrdersCountByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            
            String url = orderServiceUrl + "/api/orders?size=10000";
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                Map<?, ?> body = response.getBody();
                if (body != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
                    
                    if (content != null) {
                        return content.stream()
                            .filter(order -> {
                                Object createdAt = order.get("createdAt");
                                if (createdAt != null) {
                                    try {
                                        LocalDateTime orderDate = LocalDateTime.parse(createdAt.toString());
                                        return !orderDate.isBefore(startDateTime) && !orderDate.isAfter(endDateTime);
                                    } catch (Exception e) {
                                        return false;
                                    }
                                }
                                return false;
                            })
                            .count();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting orders count by date range: {}", e.getMessage(), e);
        }
        return 0L;
    }
    
    private Long getActiveUsersCount() {
        try {
            String url = userServiceUrl + "/api/users/stats";
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                Map<?, ?> body = response.getBody();
                if (body != null) {
                    Object activeUsers = body.get("activeUsers");
                    if (activeUsers instanceof Number) {
                        return ((Number) activeUsers).longValue();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting active users count: {}", e.getMessage(), e);
        }
        return 0L;
    }
    
    private Long getTotalUsersCount() {
        try {
            String url = userServiceUrl + "/api/users/stats";
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                Map<?, ?> body = response.getBody();
                if (body != null) {
                    Object totalUsers = body.get("totalUsers");
                    if (totalUsers instanceof Number) {
                        return ((Number) totalUsers).longValue();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting total users count: {}", e.getMessage(), e);
        }
        return 0L;
    }
    
    private Long getNewUsersCount(LocalDate sinceDate) {
        try {
            String url = userServiceUrl + "/api/users?size=10000";
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                Map<?, ?> body = response.getBody();
                if (body != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
                    
                    if (content != null) {
                        return content.stream()
                            .filter(user -> {
                                Object createdAt = user.get("createdAt");
                                if (createdAt != null) {
                                    try {
                                        LocalDateTime userDate = LocalDateTime.parse(createdAt.toString());
                                        return !userDate.toLocalDate().isBefore(sinceDate);
                                    } catch (Exception e) {
                                        return false;
                                    }
                                }
                                return false;
                            })
                            .count();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting new users count: {}", e.getMessage(), e);
        }
        return 0L;
    }
    
    private Long getRetainedUsersCount(LocalDate sinceDate) {
        try {
            // Get users who made orders in the date range
            String url = orderServiceUrl + "/api/orders?size=10000";
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                Map<?, ?> body = response.getBody();
                if (body != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
                    
                    if (content != null) {
                        Set<String> userIds = new HashSet<>();
                        LocalDateTime sinceDateTime = sinceDate.atStartOfDay();
                        
                        content.stream()
                            .filter(order -> {
                                Object createdAt = order.get("createdAt");
                                if (createdAt != null) {
                                    try {
                                        LocalDateTime orderDate = LocalDateTime.parse(createdAt.toString());
                                        return !orderDate.isBefore(sinceDateTime);
                                    } catch (Exception e) {
                                        return false;
                                    }
                                }
                                return false;
                            })
                            .forEach(order -> {
                                Object userId = order.get("userId");
                                if (userId != null) {
                                    userIds.add(userId.toString());
                                }
                            });
                        
                        return (long) userIds.size();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting retained users count: {}", e.getMessage(), e);
        }
        return 0L;
    }
    
    private HttpHeaders createServiceHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String credentials = interServiceUsername + ":" + interServicePassword;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        headers.set("Authorization", "Basic " + encodedCredentials);
        return headers;
    }
}

