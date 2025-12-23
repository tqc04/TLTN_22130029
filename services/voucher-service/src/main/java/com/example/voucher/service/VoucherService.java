package com.example.voucher.service;

import com.example.voucher.dto.VoucherValidationRequest;
import com.example.voucher.dto.VoucherValidationResponse;
import com.example.voucher.dto.VoucherUsageRequest;
import com.example.voucher.entity.UserVoucher;
import com.example.voucher.entity.Voucher;
import com.example.voucher.entity.VoucherUsage;
import com.example.voucher.event.VoucherEventPublisher;
import com.example.voucher.repository.UserVoucherRepository;
import com.example.voucher.repository.VoucherRepository;
import com.example.voucher.repository.VoucherUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VoucherService {
    
    private static final Logger logger = LoggerFactory.getLogger(VoucherService.class);

    @Autowired
    private VoucherRepository voucherRepository;
    
    @Autowired
    private VoucherUsageRepository voucherUsageRepository;
    
    @Autowired
    private UserVoucherRepository userVoucherRepository;

    @Autowired
    private VoucherEventPublisher eventPublisher;

    /**
     * Validate and apply voucher to cart/order
     */
    public VoucherValidationResponse validateAndApplyVoucher(VoucherValidationRequest request) {
        try {
            // Find voucher by code (cache disabled)
            Voucher voucher = getVoucherByCodeCached(request.getVoucherCode());
            if (voucher == null) {
                return new VoucherValidationResponse(false, "Voucher code not found or inactive");
            }

            // Validate voucher business rules
            VoucherValidationResponse response = validateVoucherBusinessRules(voucher, request);
            return response;

        } catch (Exception e) {
            logger.error("Error validating voucher {}: {}", request.getVoucherCode(), e.getMessage(), e);
            return new VoucherValidationResponse(false, "Error validating voucher: " + e.getMessage());
        }
    }

    /**
     * Record voucher usage when order is placed (Redis disabled - using database only)
     */
    @Transactional
    public VoucherUsage recordVoucherUsage(VoucherUsageRequest request) {
        try {
            // Get current voucher
            Voucher voucher = getVoucherByIdCached(request.getVoucherId());
            if (voucher == null) {
                throw new IllegalArgumentException("Voucher not found");
            }

            // Double-check validation before usage
            VoucherValidationRequest validationRequest = new VoucherValidationRequest();
            validationRequest.setVoucherCode(request.getVoucherCode());
            validationRequest.setUserId(request.getUserId());
            validationRequest.setOrderAmount(request.getOriginalAmount());

            VoucherValidationResponse validation = validateVoucherBusinessRules(voucher, validationRequest);
            if (!validation.isValid()) {
                throw new IllegalStateException("Voucher validation failed: " + validation.getMessage());
            }

            // Check usage limit (from database, no Redis cache)
            if (voucher.getUsageLimit() != null && voucher.getUsageCount() >= voucher.getUsageLimit()) {
                throw new IllegalStateException("Voucher usage limit exceeded");
            }

        // Mark user voucher as used
        if (request.getUserId() != null && !request.getUserId().isEmpty()) {
            markUserVoucherAsUsed(request.getUserId(), request.getVoucherId(), 
                request.getOrderId(), request.getOrderNumber());
        }
        
        // Create usage record
            VoucherUsage usage = new VoucherUsage(
                request.getVoucherId(),
                request.getVoucherCode(),
                request.getUserId(),
                request.getOrderId(),
                request.getOrderNumber(),
                request.getOriginalAmount(),
                request.getDiscountAmount(),
                request.getFinalAmount()
            );

            // Save to database
            VoucherUsage savedUsage = voucherUsageRepository.save(usage);

            // Update voucher usage count
            voucher.incrementUsage();
            voucherRepository.save(voucher);

            // Update cache
            updateVoucherCache(request.getVoucherId());

            // Clear validation cache since voucher state changed
            clearVoucherValidationCache(request.getVoucherCode());

            // Publish voucher used event
            eventPublisher.publishVoucherUsedEvent(savedUsage);

            logger.info("Successfully recorded voucher usage: {} for order {}, discount: {}",
                       request.getVoucherCode(), request.getOrderNumber(), request.getDiscountAmount());

            return savedUsage;

        } catch (Exception e) {
            logger.error("Error recording voucher usage: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get voucher by code (cache disabled)
     */
    public Voucher getVoucherByCodeCached(String code) {
        Optional<Voucher> voucherOpt = voucherRepository.findByCodeAndIsActiveTrue(code.toUpperCase());
        return voucherOpt.orElse(null);
    }

    /**
     * Get voucher by ID (cache disabled)
     */
    public Voucher getVoucherByIdCached(Long id) {
        return voucherRepository.findById(id).orElse(null);
    }

    /**
     * Validate voucher business rules
     */
    private VoucherValidationResponse validateVoucherBusinessRules(Voucher voucher, VoucherValidationRequest request) {
        // Check if user has claimed this voucher (if required)
        if (request.getUserId() != null && !request.getUserId().isEmpty()) {
            Optional<UserVoucher> userVoucherOpt = userVoucherRepository.findByUserIdAndVoucherId(
                request.getUserId(), voucher.getId());
            if (userVoucherOpt.isEmpty()) {
                return new VoucherValidationResponse(false, 
                    "Bạn chưa lấy voucher này. Vui lòng lấy voucher trước khi sử dụng.");
            }
            
            UserVoucher userVoucher = userVoucherOpt.get();
            if (userVoucher.getIsUsed()) {
                return new VoucherValidationResponse(false, "Voucher này đã được sử dụng");
            }
            
            // Check if voucher is expired
            if (userVoucher.isExpired(voucher.getEndDate())) {
                return new VoucherValidationResponse(false, "Voucher đã hết hạn");
            }
        }
        
        // Check if voucher is valid (dates, usage limits)
        if (!isVoucherCurrentlyValid(voucher)) {
            return new VoucherValidationResponse(false, "Voucher has expired or usage limit exceeded");
        }
        
        // Check minimum order amount
        if (voucher.getMinOrderAmount() != null &&
            request.getOrderAmount().compareTo(voucher.getMinOrderAmount()) < 0) {
            return new VoucherValidationResponse(false,
                String.format("Minimum order amount required: %s", voucher.getMinOrderAmount()));
        }
        
        // Check user-specific usage limit
        if (voucher.getUsageLimitPerUser() != null && request.getUserId() != null) {
            Long userUsageCount = voucherUsageRepository.countUsageByVoucherIdAndUserId(voucher.getId(), request.getUserId());
            if (userUsageCount >= voucher.getUsageLimitPerUser()) {
                return new VoucherValidationResponse(false,
                    "You have reached the maximum usage limit for this voucher");
            }
        }

        // Check if items are applicable (simplified - in real implementation, check against product/brand/category)
        if (!isApplicableToItems(voucher, request)) {
            return new VoucherValidationResponse(false, "Voucher not applicable to selected items");
        }
        
        // Calculate discount
        BigDecimal discountAmount = voucher.calculateDiscount(request.getOrderAmount());
        BigDecimal finalAmount = request.getOrderAmount().subtract(discountAmount);

        return new VoucherValidationResponse(
            true,
            "Voucher applied successfully",
            discountAmount,
            finalAmount,
            voucher.getId(),
            voucher.getCode(),
            voucher.getType(),
            voucher.getValue(),
            voucher.getFreeShipping() != null ? voucher.getFreeShipping() : false
        );
    }

    /**
     * Check if voucher is currently valid
     */
    private boolean isVoucherCurrentlyValid(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        return voucher.isActive() &&
               (voucher.getStartDate() == null || !now.isBefore(voucher.getStartDate())) &&
               (voucher.getEndDate() == null || now.isBefore(voucher.getEndDate())) &&
               (voucher.getUsageLimit() == null || voucher.getUsageCount() < voucher.getUsageLimit());
    }

    /**
     * Check if voucher is applicable to items
     */
    private boolean isApplicableToItems(Voucher voucher, VoucherValidationRequest request) {
        // Simplified implementation - in real scenario, check against actual products
        // For now, assume all vouchers are applicable unless specified otherwise
        if (voucher.getApplicableTo() == null || "ALL".equals(voucher.getApplicableTo())) {
            return true;
        }


        return true;
    }

    /**
     * Clear voucher validation cache (no-op, cache disabled)
     */
    public void clearVoucherValidationCache(String voucherCode) {
        // No-op: cache disabled
    }

    /**
     * Update voucher cache (no-op, cache disabled)
     */
    public void updateVoucherCache(Long voucherId) {
        // No-op: cache disabled
    }

    // Admin methods
    public Voucher createVoucher(Voucher voucher) {
        voucher.setCode(voucher.getCode().toUpperCase());
        voucher.setCreatedAt(LocalDateTime.now());
        voucher.setUpdatedAt(LocalDateTime.now());
        
        if (voucherRepository.findByCode(voucher.getCode()).isPresent()) {
            throw new IllegalArgumentException("Voucher code already exists");
        }
        
        Voucher saved = voucherRepository.save(voucher);
        logger.info("Created new voucher: {}", voucher.getCode());
        return saved;
    }

    /**
     * Get active public vouchers (not expired, active, and public)
     * Professional e-commerce: Only show vouchers that are currently valid
     */
    public List<Voucher> getActivePublicVouchers() {
        LocalDateTime now = LocalDateTime.now();
        return voucherRepository.findActivePublicVouchers(now);
    }
    
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    public List<VoucherUsage> getUserVoucherUsage(String userId) {
        return voucherUsageRepository.findByUserIdOrderByUsedAtDesc(userId);
    }

    public Optional<VoucherUsage> getVoucherUsageByOrderId(Long orderId) {
        return voucherUsageRepository.findByOrderId(orderId);
    }

    public Optional<VoucherUsage> getVoucherUsageByOrderNumber(String orderNumber) {
        return voucherUsageRepository.findByOrderNumber(orderNumber);
    }

    // Additional methods needed by controller
    public Voucher updateVoucher(Long voucherId, Voucher updatedVoucher) {
        Voucher existingVoucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
        
        // Update fields
        existingVoucher.setName(updatedVoucher.getName());
        existingVoucher.setDescription(updatedVoucher.getDescription());
        existingVoucher.setType(updatedVoucher.getType());
        existingVoucher.setValue(updatedVoucher.getValue());
        existingVoucher.setMinOrderAmount(updatedVoucher.getMinOrderAmount());
        existingVoucher.setMaxDiscountAmount(updatedVoucher.getMaxDiscountAmount());
        existingVoucher.setStartDate(updatedVoucher.getStartDate());
        existingVoucher.setEndDate(updatedVoucher.getEndDate());
        existingVoucher.setUsageLimit(updatedVoucher.getUsageLimit());
        existingVoucher.setUsageLimitPerUser(updatedVoucher.getUsageLimitPerUser());
        existingVoucher.setActive(updatedVoucher.isActive());
        existingVoucher.setPublic(updatedVoucher.isPublic());
        existingVoucher.setApplicableTo(updatedVoucher.getApplicableTo());
        existingVoucher.setApplicableItems(updatedVoucher.getApplicableItems());
        existingVoucher.setUpdatedAt(LocalDateTime.now());

        Voucher saved = voucherRepository.save(existingVoucher);

        // Clear caches
        clearVoucherValidationCache(saved.getCode());
        updateVoucherCache(saved.getId());
        
        logger.info("Updated voucher: {}", existingVoucher.getCode());
        return saved;
    }

    public void deactivateVoucher(Long voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
        
        voucher.setActive(false);
        voucher.setUpdatedAt(LocalDateTime.now());
        
        voucherRepository.save(voucher);

        // Clear caches
        clearVoucherValidationCache(voucher.getCode());
        updateVoucherCache(voucher.getId());
        
        logger.info("Deactivated voucher: {}", voucher.getCode());
    }

    public boolean canUserUseVoucher(String voucherCode, String userId) {
        Voucher voucher = getVoucherByCodeCached(voucherCode.toUpperCase());
        
        if (voucher == null) {
            return false;
        }
        
        if (!isVoucherCurrentlyValid(voucher)) {
            return false;
        }
        
        // Check if user has claimed this voucher
        if (userId != null && !userId.isEmpty()) {
            Optional<UserVoucher> userVoucherOpt = userVoucherRepository.findByUserIdAndVoucherId(userId, voucher.getId());
            if (userVoucherOpt.isEmpty() || userVoucherOpt.get().getIsUsed()) {
                return false;
            }
        }
        
        // Check user-specific usage limit
        if (voucher.getUsageLimitPerUser() != null && userId != null && !userId.isEmpty()) {
            Long userUsageCount = voucherUsageRepository.countUsageByVoucherIdAndUserId(voucher.getId(), userId);
            return userUsageCount < voucher.getUsageLimitPerUser();
        }
        
        return true;
    }
    
    /**
     * Claim/Obtain a voucher for a user
     */
    @Transactional
    public UserVoucher claimVoucher(String userId, Long voucherId) {
        logger.info("Claiming voucher {} for user {}", voucherId, userId);
        
        // Check if user already has this voucher
        Optional<UserVoucher> existingOpt = userVoucherRepository.findByUserIdAndVoucherId(userId, voucherId);
        if (existingOpt.isPresent()) {
            UserVoucher existing = existingOpt.get();
            logger.warn("User {} already has voucher {} (isUsed: {})", userId, voucherId, existing.getIsUsed());
            throw new IllegalStateException("Bạn đã lấy voucher này rồi");
        }
        
        // Get voucher
        Voucher voucher = getVoucherByIdCached(voucherId);
        if (voucher == null) {
            logger.error("Voucher {} not found", voucherId);
            throw new IllegalArgumentException("Voucher not found");
        }
        
        // Check if voucher is available
        if (!voucher.isActive() || !voucher.isPublic()) {
            logger.warn("Voucher {} is not available (active: {}, public: {})", 
                voucherId, voucher.isActive(), voucher.isPublic());
            throw new IllegalStateException("Voucher không khả dụng");
        }
        
        // Check if voucher is still valid
        if (!isVoucherCurrentlyValid(voucher)) {
            logger.warn("Voucher {} is not currently valid", voucherId);
            throw new IllegalStateException("Voucher đã hết hạn hoặc đã hết lượt sử dụng");
        }
        
        // Check usage limit
        if (voucher.getUsageLimit() != null) {
            Long claimedCount = userVoucherRepository.countUnusedByVoucherId(voucherId);
            if (claimedCount >= voucher.getUsageLimit()) {
                logger.warn("Voucher {} has reached usage limit (claimed: {}, limit: {})", 
                    voucherId, claimedCount, voucher.getUsageLimit());
                throw new IllegalStateException("Voucher đã hết lượt lấy");
            }
        }
        
        // Create UserVoucher
        UserVoucher userVoucher = new UserVoucher(userId, voucherId, voucher.getCode());
        userVoucher = userVoucherRepository.save(userVoucher);
        
        logger.info("User {} successfully claimed voucher {} (userVoucherId: {})", 
            userId, voucher.getCode(), userVoucher.getId());
        
        // Flush to ensure it's persisted before returning
        userVoucherRepository.flush();
        
        return userVoucher;
    }
    
    /**
     * Get available vouchers for a user (vouchers that user has claimed and not used)
     */
    public List<UserVoucher> getUserAvailableVouchers(String userId) {
        logger.debug("Getting available vouchers for user {}", userId);
        
        // First, get all unused vouchers for the user
        List<UserVoucher> allUnused = userVoucherRepository.findByUserIdAndIsUsedFalseOrderByObtainedAtDesc(userId);
        logger.debug("Found {} unused vouchers for user {} (before filtering)", allUnused.size(), userId);
        
        // Filter to only include vouchers that are still valid
        LocalDateTime now = LocalDateTime.now();
        List<UserVoucher> result = allUnused.stream()
            .filter(uv -> {
                Voucher voucher = getVoucherByIdCached(uv.getVoucherId());
                if (voucher == null) {
                    logger.warn("Voucher {} not found for userVoucher {}", uv.getVoucherId(), uv.getId());
                    return false;
                }
                // Check if voucher is active and within valid date range
                boolean isValid = voucher.isActive() 
                    && (voucher.getStartDate() == null || voucher.getStartDate().isBefore(now) || voucher.getStartDate().isEqual(now))
                    && (voucher.getEndDate() == null || voucher.getEndDate().isAfter(now));
                
                if (!isValid) {
                    logger.debug("Voucher {} is not valid (active: {}, startDate: {}, endDate: {})", 
                        uv.getVoucherId(), voucher.isActive(), voucher.getStartDate(), voucher.getEndDate());
                }
                return isValid;
            })
            .collect(java.util.stream.Collectors.toList());
        
        logger.debug("Found {} available vouchers for user {} (after filtering)", result.size(), userId);
        return result;
    }
    
    /**
     * Get all vouchers for a user (including used ones)
     */
    public List<UserVoucher> getUserVouchers(String userId) {
        return userVoucherRepository.findByUserIdOrderByObtainedAtDesc(userId);
    }
    
    /**
     * Mark user voucher as used when order is placed
     */
    @Transactional
    public void markUserVoucherAsUsed(String userId, Long voucherId, Long orderId, String orderNumber) {
        Optional<UserVoucher> userVoucherOpt = userVoucherRepository.findByUserIdAndVoucherId(userId, voucherId);
        if (userVoucherOpt.isPresent()) {
            UserVoucher userVoucher = userVoucherOpt.get();
            if (!userVoucher.getIsUsed()) {
                userVoucher.markAsUsed(orderId, orderNumber);
                userVoucherRepository.save(userVoucher);
                logger.info("Marked user voucher {} as used for order {}", voucherId, orderNumber);
            }
        }
    }
    
}
