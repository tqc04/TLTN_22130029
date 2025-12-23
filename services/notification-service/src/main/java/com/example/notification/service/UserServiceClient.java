package com.example.notification.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    Map<String, Object> getUserById(@PathVariable String userId);

    @GetMapping("/api/users/role/ADMIN")
    List<Map<String, Object>> getAdminUsers();
}
