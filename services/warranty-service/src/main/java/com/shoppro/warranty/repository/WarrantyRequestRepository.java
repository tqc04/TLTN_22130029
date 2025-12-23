package com.shoppro.warranty.repository;

import com.shoppro.warranty.entity.WarrantyRequest;
import com.shoppro.warranty.entity.WarrantyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyRequestRepository extends JpaRepository<WarrantyRequest, Long> {

    Optional<WarrantyRequest> findByRequestNumber(String requestNumber);

    Page<WarrantyRequest> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<WarrantyRequest> findByStatusOrderByCreatedAtDesc(WarrantyStatus status, Pageable pageable);

    Page<WarrantyRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<WarrantyRequest> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, WarrantyStatus status, Pageable pageable);

    List<WarrantyRequest> findByOrderId(Long orderId);

    List<WarrantyRequest> findByProductIdAndUserId(String productId, String userId);

    @Query("SELECT wr FROM WarrantyRequest wr WHERE wr.userId = :userId AND wr.status IN :statuses ORDER BY wr.createdAt DESC")
    Page<WarrantyRequest> findByUserIdAndStatusIn(@Param("userId") String userId,
                                                 @Param("statuses") List<WarrantyStatus> statuses,
                                                 Pageable pageable);

    @Query("SELECT wr FROM WarrantyRequest wr WHERE wr.warrantyEndDate < :currentDate AND wr.status = :status")
    List<WarrantyRequest> findExpiredWarrantyRequests(@Param("currentDate") LocalDateTime currentDate,
                                                      @Param("status") WarrantyStatus status);

    @Query("SELECT COUNT(wr) FROM WarrantyRequest wr WHERE wr.status = :status")
    long countByStatus(@Param("status") WarrantyStatus status);

    @Query("SELECT COUNT(wr) FROM WarrantyRequest wr WHERE wr.userId = :userId")
    long countByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(wr) FROM WarrantyRequest wr WHERE wr.createdAt >= :startDate AND wr.createdAt <= :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
}
