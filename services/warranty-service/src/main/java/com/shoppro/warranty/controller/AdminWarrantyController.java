package com.shoppro.warranty.controller;

import com.shoppro.warranty.dto.WarrantyRequestDTO;
import com.shoppro.warranty.entity.WarrantyStatus;
import com.shoppro.warranty.service.WarrantyRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/warranty")
@PreAuthorize("hasRole('ADMIN') or hasRole('REPAIR_TECHNICIAN')")
public class AdminWarrantyController {

    @Autowired
    private WarrantyRequestService warrantyRequestService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Lấy tất cả yêu cầu bảo hành (admin)
     */
    @GetMapping("/requests")
    public ResponseEntity<Map<String, Object>> getAllWarrantyRequests(
            Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId) {

        try {
            Page<WarrantyRequestDTO> requests;

            if (status != null && !status.isEmpty()) {
                try {
                    WarrantyStatus statusEnum = WarrantyStatus.valueOf(status.toUpperCase());
                    if (userId != null && !userId.isEmpty()) {
                        // Filter by user and status
                        requests = warrantyRequestService.getWarrantyRequestsByUserAndStatus(userId, statusEnum, pageable);
                    } else {
                        // Filter by status only
                        requests = warrantyRequestService.getWarrantyRequestsByStatus(statusEnum, pageable);
                    }
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "error", "Invalid status: " + status
                    ));
                }
            } else if (userId != null) {
                // Filter by user only
                requests = warrantyRequestService.getWarrantyRequestsByUser(userId, pageable);
            } else {
                // Get all requests
                requests = warrantyRequestService.getAllWarrantyRequests(pageable);
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
     * Lấy chi tiết yêu cầu bảo hành (admin)
     */
    @GetMapping("/requests/{id}")
    public ResponseEntity<Map<String, Object>> getWarrantyRequest(@PathVariable Long id) {
        try {
            WarrantyRequestDTO request = warrantyRequestService.getWarrantyRequest(id).orElse(null);

            if (request == null) {
                return ResponseEntity.notFound().build();
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
     * Cập nhật trạng thái yêu cầu bảo hành (admin)
     */
    @PutMapping("/requests/{id}/status")
    public ResponseEntity<Map<String, Object>> updateWarrantyStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String statusStr = body.get("status");
        String notes = body.get("notes");

        try {
            if (statusStr == null || statusStr.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Status is required"
                ));
            }

            WarrantyStatus newStatus = WarrantyStatus.valueOf(statusStr.toUpperCase());
            WarrantyRequestDTO updatedRequest = warrantyRequestService.updateWarrantyStatus(id, newStatus, notes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Warranty status updated successfully",
                    "data", updatedRequest
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Invalid status: " + statusStr
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Lấy thống kê bảo hành (admin)
     */
    @GetMapping("/stats")
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
     * Bulk update warranty requests status (admin)
     */
    @PutMapping("/requests/bulk/status")
    public ResponseEntity<Map<String, Object>> bulkUpdateStatus(
            @RequestBody Map<String, Object> body) {

        try {
            // Validate required fields
            if (!body.containsKey("requestIds") || !body.containsKey("status")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "requestIds and status are required"
                ));
            }

            // Extract request IDs
            Object requestIdsObj = body.get("requestIds");
            if (!(requestIdsObj instanceof List)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "requestIds must be an array"
                ));
            }

            @SuppressWarnings("unchecked")
            List<Object> requestIdsList = (List<Object>) requestIdsObj;
            List<Long> requestIds = new java.util.ArrayList<>();
            
            for (Object id : requestIdsList) {
                if (id instanceof Number) {
                    requestIds.add(((Number) id).longValue());
                } else if (id instanceof String) {
                    try {
                        requestIds.add(Long.parseLong((String) id));
                    } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "success", false,
                                "error", "Invalid request ID format: " + id
                        ));
                    }
                }
            }

            if (requestIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "At least one request ID is required"
                ));
            }

            // Extract and validate status
            String statusStr = body.get("status").toString();
            WarrantyStatus newStatus;
            try {
                newStatus = WarrantyStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Invalid status: " + statusStr
                ));
            }

            // Extract notes (optional)
            String notes = body.containsKey("notes") ? body.get("notes").toString() : null;

            // Perform bulk update
            Map<String, Object> result = warrantyRequestService.bulkUpdateWarrantyStatus(requestIds, newStatus, notes);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
