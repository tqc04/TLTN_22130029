package com.example.voucher.repository;

import com.example.voucher.entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {
    
    Optional<UserVoucher> findByUserIdAndVoucherId(String userId, Long voucherId);
    
    Optional<UserVoucher> findByUserIdAndVoucherCode(String userId, String voucherCode);
    
    List<UserVoucher> findByUserIdAndIsUsedFalseOrderByObtainedAtDesc(String userId);
    
    List<UserVoucher> findByUserIdOrderByObtainedAtDesc(String userId);
    
    @Query("SELECT uv FROM UserVoucher uv WHERE uv.userId = :userId AND uv.isUsed = false " +
           "AND EXISTS (SELECT v FROM Voucher v WHERE v.id = uv.voucherId AND v.isActive = true " +
           "AND (v.startDate IS NULL OR v.startDate <= :now) " +
           "AND (v.endDate IS NULL OR v.endDate > :now)) " +
           "ORDER BY uv.obtainedAt DESC")
    List<UserVoucher> findAvailableVouchersForUser(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(uv) FROM UserVoucher uv WHERE uv.voucherId = :voucherId AND uv.isUsed = false")
    Long countUnusedByVoucherId(@Param("voucherId") Long voucherId);
    
    @Query("SELECT COUNT(uv) FROM UserVoucher uv WHERE uv.userId = :userId AND uv.voucherId = :voucherId")
    Long countByUserIdAndVoucherId(@Param("userId") String userId, @Param("voucherId") Long voucherId);
}

