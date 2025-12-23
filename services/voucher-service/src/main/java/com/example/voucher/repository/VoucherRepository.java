package com.example.voucher.repository;

import com.example.voucher.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    
    Optional<Voucher> findByCode(String code);
    
    Optional<Voucher> findByCodeAndIsActiveTrue(String code);
    
    List<Voucher> findByIsActiveTrueAndIsPublicTrueOrderByCreatedAtDesc();
    
    List<Voucher> findByIsActiveTrueAndStartDateBeforeAndEndDateAfterOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT v FROM Voucher v WHERE v.isActive = true AND v.isPublic = true " +
           "AND (v.startDate IS NULL OR v.startDate <= :now) " +
           "AND (v.endDate IS NULL OR v.endDate > :now) " +
           "ORDER BY v.createdAt DESC")
    List<Voucher> findActivePublicVouchers(@Param("now") LocalDateTime now);
    
    /**
     * Find vouchers that have expired (for scheduled task)
     */
    @Query("SELECT v FROM Voucher v WHERE v.isActive = true " +
           "AND v.endDate IS NOT NULL " +
           "AND v.endDate <= :now")
    List<Voucher> findExpiredActiveVouchers(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(vu) FROM VoucherUsage vu WHERE vu.voucherId = :voucherId AND vu.userId = :userId")
    Long countUsageByVoucherAndUser(@Param("voucherId") Long voucherId, @Param("userId") String userId);
}
