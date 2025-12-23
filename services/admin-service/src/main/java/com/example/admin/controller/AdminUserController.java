package com.example.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${services.user.base-url:http://localhost:8082}")
    private String userServiceUrl;

    @GetMapping("")
    public ResponseEntity<?> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false, name = "q") String query,
                                  HttpServletRequest request) {
        String url = userServiceUrl + "/api/users?page=" + page + "&size=" + size;
        if (query != null && !query.isEmpty()) {
            url += "&q=" + query;
        }
        HttpHeaders headers = new HttpHeaders();
        String auth = request.getHeader("Authorization");
        if (auth != null) headers.set("Authorization", auth);
        ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        String auth = request.getHeader("Authorization");
        if (auth != null) headers.set("Authorization", auth);
        ResponseEntity<?> response = restTemplate.exchange(
                userServiceUrl + "/api/users/register",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") String userId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        HttpHeaders headers = new HttpHeaders();
        if (auth != null) headers.set("Authorization", auth);

        // Update profile fields
        String profileUrl = userServiceUrl + "/api/users/profile?userId=" + userId;
        restTemplate.exchange(profileUrl, HttpMethod.PUT, new HttpEntity<>(body, headers), Void.class);

        // If role provided, forward role change explicitly to user-service
        Object roleObj = body.get("role");
        if (roleObj != null) {
            String role = String.valueOf(roleObj);
            String roleUrl = userServiceUrl + "/api/users/" + userId + "/role";
            restTemplate.exchange(roleUrl, HttpMethod.POST, new HttpEntity<>(Map.of("role", role), headers), Map.class);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") String userId, HttpServletRequest request) {
        String url = userServiceUrl + "/api/users/" + userId;
        HttpHeaders headers = new HttpHeaders();
        String auth = request.getHeader("Authorization");
        if (auth != null) headers.set("Authorization", auth);
        restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        return ResponseEntity.ok(Map.of("success", true));
    }
}


