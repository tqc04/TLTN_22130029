package com.shoppro.warranty.service;

import com.shoppro.warranty.dto.CreateWarrantyRequestDTO;
import com.shoppro.warranty.dto.WarrantyRequestDTO;
import com.shoppro.warranty.dto.WarrantyStatsDTO;
import com.shoppro.warranty.entity.WarrantyPriority;
import com.shoppro.warranty.entity.WarrantyRequest;
import com.shoppro.warranty.entity.WarrantyStatus;
import com.shoppro.warranty.events.WarrantyEventPublisher;
import com.shoppro.warranty.repository.WarrantyRequestRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class WarrantyRequestService {

    @Autowired
    private WarrantyRequestRepository warrantyRequestRepository;

    @Autowired
    private WarrantyEventPublisher eventPublisher;

    @Autowired
    private OrderServiceClient orderServiceClient;

    @Autowired
    private RedisCacheService redisCacheService;

    /**
     * Tạo yêu cầu bảo hành mới
     */
    @Timed(value = "warranty.request.create", percentiles = {0.5, 0.95})
    public WarrantyRequestDTO createWarrantyRequest(CreateWarrantyRequestDTO createDTO) {
        // Validate order exists and is eligible for warranty
        validateWarrantyEligibility(createDTO);

        // Generate unique request number
        String requestNumber = generateRequestNumber();

        // Create warranty request entity
        WarrantyRequest warrantyRequest = new WarrantyRequest(
                requestNumber,
                createDTO.getUserId(),
                createDTO.getCustomerName(),
                createDTO.getCustomerEmail(),
                createDTO.getOrderId(),
                createDTO.getOrderNumber(),
                createDTO.getProductId(),
                createDTO.getProductName(),
                createDTO.getIssueDescription()
        );

        // Set additional fields
        warrantyRequest.setCustomerPhone(createDTO.getCustomerPhone());
        warrantyRequest.setProductSku(createDTO.getProductSku());

        // Set priority (default to NORMAL if not specified)
        if (createDTO.getPriority() != null) {
            try {
                warrantyRequest.setPriority(WarrantyPriority.valueOf(createDTO.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                warrantyRequest.setPriority(WarrantyPriority.NORMAL);
            }
        }

        // Save to database
        WarrantyRequest savedRequest = warrantyRequestRepository.save(warrantyRequest);

        // Cache the request
        redisCacheService.cacheWarrantyRequest(savedRequest);

        // Publish event to Kafka
        eventPublisher.publishWarrantyRequestedEvent(savedRequest);

        return WarrantyRequestDTO.from(savedRequest);
    }

    /**
     * Lấy danh sách yêu cầu bảo hành của user
     */
    @Transactional(readOnly = true)
    @Timed(value = "warranty.request.user.list", percentiles = {0.5, 0.95})
    public Page<WarrantyRequestDTO> getUserWarrantyRequests(String userId, Pageable pageable) {
        // First try to get from Redis cache
        Optional<List<WarrantyRequestDTO>> cachedRequests = redisCacheService.getCachedUserWarrantyRequests(
                userId, pageable.getPageNumber(), pageable.getPageSize());

        if (cachedRequests.isPresent()) {
            System.out.println("User warranty requests retrieved from Redis cache");
            // Convert List to Page (simplified for this example)
            List<WarrantyRequestDTO> requests = cachedRequests.get();
            return new org.springframework.data.domain.PageImpl<>(
                    requests, pageable, requests.size());
        }

        // Get from database
        Page<WarrantyRequest> requests = warrantyRequestRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // Cache the results
        if (requests.hasContent()) {
            List<WarrantyRequestDTO> requestDTOs = requests.getContent().stream()
                    .map(WarrantyRequestDTO::from)
                    .toList();
            redisCacheService.cacheUserWarrantyRequests(userId, requestDTOs,
                    pageable.getPageNumber(), pageable.getPageSize());
            System.out.println("User warranty requests cached in Redis");
        }

        return requests.map(WarrantyRequestDTO::from);
    }

    /**
     * Lấy danh sách yêu cầu bảo hành của user theo trạng thái
     */
    @Transactional(readOnly = true)
    @Timed(value = "warranty.request.user.listByStatus", percentiles = {0.5, 0.95})
    public Page<WarrantyRequestDTO> getUserWarrantyRequests(String userId, WarrantyStatus status, Pageable pageable) {
        Page<WarrantyRequest> requests = warrantyRequestRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        return requests.map(WarrantyRequestDTO::from);
    }

    /**
     * Lấy yêu cầu bảo hành theo ID
     */
    @Transactional(readOnly = true)
    @Timed(value = "warranty.request.get", percentiles = {0.5, 0.95})
    public Optional<WarrantyRequestDTO> getWarrantyRequest(Long id) {
        // Try cache first
        Optional<WarrantyRequestDTO> cached = redisCacheService.getCachedWarrantyRequest(id);
        if (cached.isPresent()) {
            return cached;
        }

        // If not in cache, get from database
        Optional<WarrantyRequest> request = warrantyRequestRepository.findById(id);
        if (request.isPresent()) {
            WarrantyRequestDTO dto = WarrantyRequestDTO.from(request.get());
            // Cache it
            redisCacheService.cacheWarrantyRequest(request.get());
            return Optional.of(dto);
        }

        return Optional.empty();
    }

    /**
     * Lấy yêu cầu bảo hành theo request number
     */
    @Transactional(readOnly = true)
    @Timed(value = "warranty.request.getByNumber", percentiles = {0.5, 0.95})
    public Optional<WarrantyRequestDTO> getWarrantyRequestByNumber(String requestNumber) {
        Optional<WarrantyRequest> request = warrantyRequestRepository.findByRequestNumber(requestNumber);
        return request.map(WarrantyRequestDTO::from);
    }

    /**
     * Lấy tất cả yêu cầu bảo hành (admin)
     */
    @Transactional(readOnly = true)
    @Timed(value = "warranty.request.admin.listAll", percentiles = {0.5, 0.95})
    public Page<WarrantyRequestDTO> getAllWarrantyRequests(Pageable pageable) {
        Page<WarrantyRequest> requests = warrantyRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return requests.map(WarrantyRequestDTO::from);
    }

    /**
     * Lấy yêu cầu bảo hành theo user (admin)
     */
    @Transactional(readOnly = true)
    @Timed(value = "warranty.request.admin.listByUser", percentiles = {0.5, 0.95})
    public Page<WarrantyRequestDTO> getWarrantyRequestsByUser(String userId, Pageable pageable) {
        Page<WarrantyRequest> requests = warrantyRequestRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return requests.map(WarrantyRequestDTO::from);
    }

    /**
     * Lấy yêu cầu bảo hành theo trạng thái (admin)
     */
    @Transactional(readOnly = true)
    @Timed(value = "warranty.request.admin.listByStatus", percentiles = {0.5, 0.95})
    public Page<WarrantyRequestDTO> getWarrantyRequestsByStatus(WarrantyStatus status, Pageable pageable) {
        Page<WarrantyRequest> requests = warrantyRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return requests.map(WarrantyRequestDTO::from);
    }

    /**
     * Lấy yêu cầu bảo hành theo user và trạng thái (admin)
     */
    @Transactional(readOnly = true)
    @Timed(value = "warranty.request.admin.listByUserAndStatus", percentiles = {0.5, 0.95})
    public Page<WarrantyRequestDTO> getWarrantyRequestsByUserAndStatus(String userId, WarrantyStatus status, Pageable pageable) {
        Page<WarrantyRequest> requests = warrantyRequestRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        return requests.map(WarrantyRequestDTO::from);
    }

    /**
     * Cập nhật trạng thái yêu cầu bảo hành
     */
    @Timed(value = "warranty.request.updateStatus", percentiles = {0.5, 0.95})
    public WarrantyRequestDTO updateWarrantyStatus(Long id, WarrantyStatus newStatus, String notes) {
        Optional<WarrantyRequest> requestOpt = warrantyRequestRepository.findById(id);
        if (requestOpt.isEmpty()) {
            throw new RuntimeException("Warranty request not found");
        }

        WarrantyRequest request = requestOpt.get();

        // Validate status transition
        validateStatusTransition(request.getStatus(), newStatus);

        request.setStatus(newStatus);

        // Set completion date if status is COMPLETED
        if (newStatus == WarrantyStatus.COMPLETED) {
            request.setActualCompletionDate(LocalDateTime.now());
        }

        // Set rejection reason if status is REJECTED
        if (newStatus == WarrantyStatus.REJECTED && notes != null) {
            request.setRejectionReason(notes);
        }

        // Set resolution notes if provided
        if (notes != null && newStatus != WarrantyStatus.REJECTED) {
            request.setResolutionNotes(notes);
        }

        WarrantyRequest updatedRequest = warrantyRequestRepository.save(request);

        // Update cache
        redisCacheService.cacheWarrantyRequest(updatedRequest);

        // Publish status update event
        eventPublisher.publishWarrantyStatusUpdatedEvent(updatedRequest);

        return WarrantyRequestDTO.from(updatedRequest);
    }

    /**
     * Bulk update warranty requests status
     */
    @Timed(value = "warranty.request.bulkUpdateStatus", percentiles = {0.5, 0.95})
    public Map<String, Object> bulkUpdateWarrantyStatus(List<Long> requestIds, WarrantyStatus newStatus, String notes) {
        Map<String, Object> result = new HashMap<>();
        List<Long> successfulIds = new java.util.ArrayList<>();
        List<Map<String, Object>> failedUpdates = new java.util.ArrayList<>();

        for (Long id : requestIds) {
            try {
                WarrantyRequestDTO updated = updateWarrantyStatus(id, newStatus, notes);
                successfulIds.add(id);
            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("id", id);
                error.put("error", e.getMessage());
                failedUpdates.add(error);
            }
        }

        result.put("success", true);
        result.put("totalRequested", requestIds.size());
        result.put("successful", successfulIds.size());
        result.put("failed", failedUpdates.size());
        result.put("successfulIds", successfulIds);
        if (!failedUpdates.isEmpty()) {
            result.put("failedUpdates", failedUpdates);
        }

        return result;
    }

    /**
     * Lấy thống kê bảo hành
     */
    @Transactional(readOnly = true)
    @Timed(value = "warranty.request.stats", percentiles = {0.5, 0.95})
    public WarrantyStatsDTO getWarrantyStats() {
        WarrantyStatsDTO stats = new WarrantyStatsDTO();

        for (WarrantyStatus status : WarrantyStatus.values()) {
            long count = warrantyRequestRepository.countByStatus(status);
            stats.addStatusCount(status, count);
        }

        return stats;
    }

    /**
     * Validate warranty eligibility
     */
    private void validateWarrantyEligibility(CreateWarrantyRequestDTO createDTO) {
        try {
            Map<String, Object> orderInfo = null;

            // First try to get order from Redis cache
            Optional<Map<String, Object>> cachedOrder = redisCacheService.getCachedOrderDetails(createDTO.getOrderNumber());
            if (cachedOrder.isPresent()) {
                orderInfo = cachedOrder.get();
                System.out.println("Order details retrieved from Redis cache");
            } else {
                // If not in cache, get from Order Service with circuit breaker
                orderInfo = getOrderDetailsWithCircuitBreaker(createDTO.getOrderNumber());

                // Cache the order details for future use
                if (orderInfo != null) {
                    redisCacheService.cacheOrderDetails(createDTO.getOrderNumber(), orderInfo);
                    System.out.println("Order details cached in Redis");
                }
            }

            if (orderInfo == null || Boolean.FALSE.equals(orderInfo.get("success"))) {
                throw new RuntimeException("Order not found");
            }

            // Check if product is already under warranty
            List<WarrantyRequest> existingWarranties = warrantyRequestRepository.findByProductIdAndUserId(
                    createDTO.getProductId(), createDTO.getUserId());

            boolean hasActiveWarranty = existingWarranties.stream()
                    .anyMatch(w -> w.getStatus() == WarrantyStatus.PENDING ||
                                  w.getStatus() == WarrantyStatus.APPROVED ||
                                  w.getStatus() == WarrantyStatus.RECEIVED ||
                                  w.getStatus() == WarrantyStatus.IN_PROGRESS);

            if (hasActiveWarranty) {
                throw new RuntimeException("Product already has an active warranty request");
            }

        } catch (Exception e) {
            throw new RuntimeException("Warranty validation failed: " + e.getMessage());
        }
    }

    /**
     * Validate status transition
     */
    private void validateStatusTransition(WarrantyStatus currentStatus, WarrantyStatus newStatus) {
        // Define valid transitions
        switch (currentStatus) {
            case PENDING:
                if (newStatus != WarrantyStatus.APPROVED && newStatus != WarrantyStatus.REJECTED) {
                    throw new RuntimeException("Invalid status transition from PENDING to " + newStatus);
                }
                break;
            case APPROVED:
                if (newStatus != WarrantyStatus.RECEIVED && newStatus != WarrantyStatus.CANCELLED) {
                    throw new RuntimeException("Invalid status transition from APPROVED to " + newStatus);
                }
                break;
            case RECEIVED:
                if (newStatus != WarrantyStatus.IN_PROGRESS && newStatus != WarrantyStatus.REJECTED) {
                    throw new RuntimeException("Invalid status transition from RECEIVED to " + newStatus);
                }
                break;
            case IN_PROGRESS:
                if (newStatus != WarrantyStatus.COMPLETED && newStatus != WarrantyStatus.REJECTED) {
                    throw new RuntimeException("Invalid status transition from IN_PROGRESS to " + newStatus);
                }
                break;
            default:
                throw new RuntimeException("Cannot change status from " + currentStatus);
        }
    }

    /**
     * Generate unique request number
     */
    private String generateRequestNumber() {
        return "WR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Get order details with circuit breaker protection
     */
    @CircuitBreaker(name = "orderService", fallbackMethod = "orderServiceFallback")
    private Map<String, Object> getOrderDetailsWithCircuitBreaker(String orderNumber) {
        return orderServiceClient.getOrderByNumber(orderNumber);
    }

    /**
     * Fallback when order-service is unavailable
     */
    @SuppressWarnings("unused")
    private Map<String, Object> orderServiceFallback(String orderNumber, Throwable throwable) {
        return Map.of(
                "success", false,
                "fallback", true,
                "message", "order-service unavailable",
                "orderNumber", orderNumber,
                "error", throwable != null ? throwable.getMessage() : "unknown error"
        );
    }
}
