package com.example.voucher.controller;

import com.example.voucher.dto.VoucherDTO;
import com.example.voucher.dto.VoucherUsageDTO;
import com.example.voucher.dto.VoucherUsageRequest;
import com.example.voucher.dto.VoucherValidationRequest;
import com.example.voucher.dto.VoucherValidationResponse;
import com.example.voucher.entity.Voucher;
import com.example.voucher.entity.VoucherUsage;
import com.example.voucher.service.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    private static final Logger logger = LoggerFactory.getLogger(VoucherController.class);

    @Autowired
    private VoucherService voucherService;
    
    @PostMapping("/validate")
    public ResponseEntity<VoucherValidationResponse> validateVoucher(@RequestBody VoucherValidationRequest request) {
        try {
            VoucherValidationResponse result = voucherService.validateAndApplyVoucher(request);

            if (result.isValid()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("Error validating voucher: {}", e.getMessage(), e);
            VoucherValidationResponse errorResponse = new VoucherValidationResponse(false, "Error validating voucher: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/public")
    public ResponseEntity<List<VoucherDTO>> getActivePublicVouchers() {
        try {
            List<Voucher> vouchers = voucherService.getActivePublicVouchers();
            List<VoucherDTO> voucherDTOs = vouchers.stream()
                .map(VoucherDTO::from)
                .collect(Collectors.toList());
            return ResponseEntity.ok(voucherDTOs);
        } catch (Exception e) {
            logger.error("Error fetching public vouchers: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{code}")
    public ResponseEntity<VoucherDTO> getVoucherByCode(@PathVariable String code) {
        try {
            Voucher voucher = voucherService.getVoucherByCodeCached(code.toUpperCase());
            if (voucher != null) {
                return ResponseEntity.ok(VoucherDTO.from(voucher));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching voucher by code: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}/usage")
    public ResponseEntity<List<VoucherUsageDTO>> getUserVoucherUsage(@PathVariable String userId) {
        try {
            List<VoucherUsage> usage = voucherService.getUserVoucherUsage(userId);
            List<VoucherUsageDTO> usageDTOs = usage.stream()
                .map(VoucherUsageDTO::from)
                .collect(Collectors.toList());
            return ResponseEntity.ok(usageDTOs);
        } catch (Exception e) {
            logger.error("Error fetching user voucher usage: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/usage")
    public ResponseEntity<VoucherUsageDTO> recordVoucherUsage(@RequestBody VoucherUsageRequest request) {
        try {
            VoucherUsage usage = voucherService.recordVoucherUsage(request);
            return ResponseEntity.ok(VoucherUsageDTO.from(usage));
        } catch (Exception e) {
            logger.error("Error recording voucher usage: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/usage/order/{orderId}")
    public ResponseEntity<VoucherUsageDTO> getVoucherUsageByOrderId(@PathVariable Long orderId) {
        try {
            Optional<VoucherUsage> usage = voucherService.getVoucherUsageByOrderId(orderId);
            return usage.map(u -> ResponseEntity.ok(VoucherUsageDTO.from(u)))
                       .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching voucher usage by order ID: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VoucherDTO> createVoucher(@RequestBody Voucher voucher) {
        try {
            Voucher created = voucherService.createVoucher(voucher);
            return ResponseEntity.ok(VoucherDTO.from(created));
        } catch (Exception e) {
            logger.error("Error creating voucher: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VoucherDTO> updateVoucher(@PathVariable Long id, @RequestBody Voucher voucher) {
        try {
            Voucher updated = voucherService.updateVoucher(id, voucher);
            return ResponseEntity.ok(VoucherDTO.from(updated));
        } catch (Exception e) {
            logger.error("Error updating voucher: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deactivateVoucher(@PathVariable Long id) {
        try {
            voucherService.deactivateVoucher(id);
            return ResponseEntity.ok(Map.of("message", "Voucher deactivated successfully"));
        } catch (Exception e) {
            logger.error("Error deactivating voucher: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/can-use")
    public ResponseEntity<Map<String, Boolean>> canUserUseVoucher(
            @RequestParam String voucherCode,
            @RequestParam String userId) {
        try {
            boolean canUse = voucherService.canUserUseVoucher(voucherCode, userId);
            return ResponseEntity.ok(Map.of("canUse", canUse));
        } catch (Exception e) {
            logger.error("Error checking voucher usability: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Voucher API is working!");
    }
    
    /**
     * Claim/Obtain a voucher for user
     */
    @PostMapping("/user/{userId}/claim/{voucherId}")
    public ResponseEntity<Map<String, Object>> claimVoucher(
            @PathVariable String userId,
            @PathVariable Long voucherId) {
        try {
            com.example.voucher.entity.UserVoucher userVoucher = voucherService.claimVoucher(userId, voucherId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã lấy voucher thành công",
                "userVoucherId", userVoucher.getId()
            ));
        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.error("Error claiming voucher: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error claiming voucher: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Lỗi khi lấy voucher"
            ));
        }
    }
    
    /**
     * Get available vouchers for user (vouchers that user has claimed and not used)
     */
    @GetMapping("/user/{userId}/available")
    public ResponseEntity<?> getUserAvailableVouchers(@PathVariable String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User ID is required"
                ));
            }
            
            List<com.example.voucher.entity.UserVoucher> userVouchers = voucherService.getUserAvailableVouchers(userId);
            List<Map<String, Object>> result = userVouchers.stream().map(uv -> {
                Voucher voucher = voucherService.getVoucherByIdCached(uv.getVoucherId());
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("userVoucherId", uv.getId());
                map.put("voucherId", uv.getVoucherId());
                map.put("voucherCode", uv.getVoucherCode());
                map.put("obtainedAt", uv.getObtainedAt());
                map.put("isUsed", uv.getIsUsed());
                if (voucher != null) {
                    map.put("voucher", VoucherDTO.from(voucher));
                } else {
                    logger.warn("Voucher not found for voucherId: {}", uv.getVoucherId());
                    // Still return the user voucher info even if voucher is deleted
                    map.put("voucher", null);
                }
                return map;
            }).collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error fetching user available vouchers for userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error fetching available vouchers: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get all vouchers for user (including used ones)
     */
    @GetMapping("/user/{userId}/all")
    public ResponseEntity<?> getUserVouchers(@PathVariable String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User ID is required"
                ));
            }
            
            List<com.example.voucher.entity.UserVoucher> userVouchers = voucherService.getUserVouchers(userId);
            List<Map<String, Object>> result = userVouchers.stream().map(uv -> {
                Voucher voucher = voucherService.getVoucherByIdCached(uv.getVoucherId());
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("userVoucherId", uv.getId());
                map.put("voucherId", uv.getVoucherId());
                map.put("voucherCode", uv.getVoucherCode());
                map.put("obtainedAt", uv.getObtainedAt());
                map.put("isUsed", uv.getIsUsed());
                map.put("usedAt", uv.getUsedAt());
                map.put("orderId", uv.getOrderId());
                map.put("orderNumber", uv.getOrderNumber());
                if (voucher != null) {
                    map.put("voucher", VoucherDTO.from(voucher));
                } else {
                    logger.warn("Voucher not found for voucherId: {}", uv.getVoucherId());
                    // Still return the user voucher info even if voucher is deleted
                    map.put("voucher", null);
                }
                return map;
            }).collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error fetching user vouchers for userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error fetching user vouchers: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get all vouchers (admin) with pagination
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VoucherDTO>> getAllVouchers() {
        try {
            List<Voucher> vouchers = voucherService.getAllVouchers();
            List<VoucherDTO> voucherDTOs = vouchers.stream()
                .map(VoucherDTO::from)
                .collect(Collectors.toList());
            return ResponseEntity.ok(voucherDTOs);
        } catch (Exception e) {
            logger.error("Error fetching all vouchers: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
