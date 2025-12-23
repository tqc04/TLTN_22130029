package com.example.admin.controller;

import com.example.admin.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * Get overview statistics (total sales, orders, users)
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        Map<String, Object> overview = analyticsService.getOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * Get sales data by time range (7days, 30days, 90days, year)
     */
    @GetMapping("/sales")
    public ResponseEntity<Map<String, Object>> sales(
            @RequestParam(defaultValue = "30days") String timeRange) {
        Map<String, Object> sales = analyticsService.getSalesByTimeRange(timeRange);
        return ResponseEntity.ok(sales);
    }

    /**
     * Get sales statistics by period (day, week, month, year)
     */
    @GetMapping("/sales-by-period")
    public ResponseEntity<Map<String, Object>> salesByPeriod(
            @RequestParam(defaultValue = "month") String period) {
        Map<String, Object> sales = analyticsService.getSalesByPeriod(period);
        return ResponseEntity.ok(sales);
    }

    /**
     * Get customer analysis (new users, retained users)
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> users() {
        Map<String, Object> analysis = analyticsService.getCustomerAnalysis();
        return ResponseEntity.ok(analysis);
    }

    /**
     * Get top selling products
     */
    @GetMapping("/top-products")
    public ResponseEntity<Map<String, Object>> topProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(Map.of(
                "products", analyticsService.getTopSellingProducts(limit)
        ));
    }
    
    /**
     * Get product statistics
     */
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> productStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", analyticsService.getTotalProductsCount());
        stats.put("lowStock", analyticsService.getLowStockCount());
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get category distribution (sales by category)
     */
    @GetMapping("/category-distribution")
    public ResponseEntity<Map<String, Object>> categoryDistribution() {
        return ResponseEntity.ok(Map.of(
                "categories", analyticsService.getCategoryDistribution()
        ));
    }
}


