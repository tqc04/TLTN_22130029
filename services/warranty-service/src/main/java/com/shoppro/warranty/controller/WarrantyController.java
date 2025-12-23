package com.shoppro.warranty.controller;

import com.shoppro.warranty.dto.CreateWarrantyRequestDTO;
import com.shoppro.warranty.dto.WarrantyRequestDTO;
import com.shoppro.warranty.entity.WarrantyStatus;
import com.shoppro.warranty.service.WarrantyRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/warranty")
public class WarrantyController {

    @Autowired
    private WarrantyRequestService warrantyRequestService;

    @GetMapping("/health")
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Tạo yêu cầu bảo hành mới
     */
    @PostMapping("/requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createWarrantyRequest(
            @Valid @RequestBody CreateWarrantyRequestDTO createDTO,
            Authentication authentication) {

        try {
            // Extract user ID from JWT token
            String userId = extractUserIdFromAuth(authentication);

            // Set user ID in the request
            createDTO.setUserId(userId);

            WarrantyRequestDTO createdRequest = warrantyRequestService.createWarrantyRequest(createDTO);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Warranty request created successfully",
                    "data", createdRequest
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Lấy danh sách yêu cầu bảo hành của user hiện tại
     */
    @GetMapping("/requests/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyWarrantyRequests(
            Pageable pageable,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        try {
            String userId = extractUserIdFromAuth(authentication);

            Page<WarrantyRequestDTO> requests;

            if (status != null && !status.isEmpty()) {
                try {
                    WarrantyStatus statusEnum = WarrantyStatus.valueOf(status.toUpperCase());
                    requests = warrantyRequestService.getUserWarrantyRequests(userId, statusEnum, pageable);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "error", "Invalid status: " + status
                    ));
                }
            } else {
                requests = warrantyRequestService.getUserWarrantyRequests(userId, pageable);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", requests.getContent(),
                    "totalElements", requests.getTotalElements(),
                    "totalPages", requests.getTotalPages(),
                    "currentPage", requests.getNumber()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Lấy chi tiết yêu cầu bảo hành theo ID
     */
    @GetMapping("/requests/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getWarrantyRequest(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            String userId = extractUserIdFromAuth(authentication);

            // Check if user owns this warranty request
            WarrantyRequestDTO request = warrantyRequestService.getWarrantyRequest(id).orElse(null);

            if (request == null) {
                return ResponseEntity.notFound().build();
            }

            if (!request.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "Access denied"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", request
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Lấy yêu cầu bảo hành theo request number
     */
    @GetMapping("/requests/number/{requestNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getWarrantyRequestByNumber(
            @PathVariable String requestNumber,
            Authentication authentication) {

        try {
            String userId = extractUserIdFromAuth(authentication);

            WarrantyRequestDTO request = warrantyRequestService.getWarrantyRequestByNumber(requestNumber).orElse(null);

            if (request == null) {
                return ResponseEntity.notFound().build();
            }

            if (!request.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "Access denied"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", request
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Lấy thống kê bảo hành (chỉ dành cho admin)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getWarrantyStats() {
        try {
            var stats = warrantyRequestService.getWarrantyStats();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Extract user ID from authentication token
     */
    private String extractUserIdFromAuth(Authentication authentication) {
        if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) {
            org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth =
                (org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) authentication;
            org.springframework.security.oauth2.jwt.Jwt jwt = jwtAuth.getToken();

            if (jwt.hasClaim("userId")) {
                Object userIdClaim = jwt.getClaim("userId");
                if (userIdClaim instanceof String) {
                    return (String) userIdClaim;
                } else if (userIdClaim instanceof Number) {
                    return String.valueOf(userIdClaim);
                }
            }

            if (jwt.hasClaim("id")) {
                Object idClaim = jwt.getClaim("id");
                if (idClaim instanceof String) {
                    return (String) idClaim;
                } else if (idClaim instanceof Number) {
                    return String.valueOf(idClaim);
                }
            }
        }

        throw new RuntimeException("Unable to extract user ID from authentication token");
    }
}
