package com.example.admin.controller;

import com.example.admin.entity.AdminUser;
import com.example.admin.entity.AdminRole;
import com.example.admin.service.AdminService;
import com.example.admin.config.DatabaseMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private DatabaseMonitor databaseMonitor;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    /**
     * Get database status with connection pool metrics
     */
    @GetMapping("/database/status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        Map<String, Object> status = databaseMonitor.getConnectionPoolStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Get database health details
     */
    @GetMapping("/database/health")
    public ResponseEntity<Map<String, Object>> getDatabaseHealth() {
        Map<String, Object> health = databaseMonitor.getDatabaseHealth();
        boolean isHealthy = databaseMonitor.isDatabaseHealthy();
        
        if (isHealthy) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * Get comprehensive database metrics
     */
    @GetMapping("/database/metrics")
    public ResponseEntity<Map<String, Object>> getDatabaseMetrics() {
        Map<String, Object> metrics = databaseMonitor.getDatabaseMetrics();
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        try {
            String usernameOrEmail = body.getOrDefault("username", "");
            String password = body.getOrDefault("password", "");

            var adminOpt = adminService.authenticateAdmin(usernameOrEmail, password);
            if (adminOpt.isPresent()) {
                return ResponseEntity.ok(Map.of("success", true, "admin", createAdminResponse(adminOpt.get())));
            } else {
                return ResponseEntity.status(401).body(Map.of("success", false, "error", "Invalid credentials"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createAdmin(@RequestBody Map<String, Object> body) {
        try {
            String username = (String) body.get("username");
            String email = (String) body.get("email");
            String password = (String) body.get("password");
            String firstName = (String) body.get("firstName");
            String lastName = (String) body.get("lastName");
            String roleStr = (String) body.get("role");
            
            AdminRole role = AdminRole.valueOf(roleStr.toUpperCase());
            AdminUser admin = adminService.createAdminUser(username, email, password, firstName, lastName, role);
            
            return ResponseEntity.ok(Map.of("success", true, "admin", createAdminResponse(admin)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAdmin(@PathVariable Long id) {
        try {
            Optional<AdminUser> adminOpt = adminService.findById(id);
            if (adminOpt.isPresent()) {
                return ResponseEntity.ok(createAdminResponse(adminOpt.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdminStatistics() {
        try {
            return ResponseEntity.ok(adminService.getAdminStatistics());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> createAdminResponse(AdminUser admin) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", admin.getId());
        response.put("username", admin.getUsername());
        response.put("email", admin.getEmail());
        response.put("firstName", admin.getFirstName());
        response.put("lastName", admin.getLastName());
        response.put("role", admin.getRole().name());
        response.put("isActive", admin.getIsActive());
        response.put("lastLoginAt", admin.getLastLoginAt());
        response.put("createdAt", admin.getCreatedAt());
        return response;
    }
}