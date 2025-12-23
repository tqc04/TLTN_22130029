package com.example.voucher.repository;

import com.example.voucher.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    Optional<VoucherUsage> findByOrderId(Long orderId);

    Optional<VoucherUsage> findByOrderNumber(String orderNumber);

    List<VoucherUsage> findByUserIdOrderByUsedAtDesc(String userId);

    List<VoucherUsage> findByVoucherIdOrderByUsedAtDesc(Long voucherId);

    @Query("SELECT vu FROM VoucherUsage vu WHERE vu.voucherId = :voucherId AND vu.userId = :userId ORDER BY vu.usedAt DESC")
    List<VoucherUsage> findByVoucherIdAndUserIdOrderByUsedAtDesc(@Param("voucherId") Long voucherId, @Param("userId") String userId);

    @Query("SELECT COUNT(vu) FROM VoucherUsage vu WHERE vu.voucherId = :voucherId")
    Long countUsageByVoucherId(@Param("voucherId") Long voucherId);

    @Query("SELECT COUNT(vu) FROM VoucherUsage vu WHERE vu.userId = :userId")
    Long countUsageByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(vu) FROM VoucherUsage vu WHERE vu.voucherId = :voucherId AND vu.userId = :userId")
    Long countUsageByVoucherIdAndUserId(@Param("voucherId") Long voucherId, @Param("userId") String userId);

    boolean existsByOrderId(Long orderId);

    boolean existsByOrderNumber(String orderNumber);
}