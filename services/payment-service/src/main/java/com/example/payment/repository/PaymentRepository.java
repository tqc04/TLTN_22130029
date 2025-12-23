package com.example.payment.repository;

import com.example.payment.entity.Payment;
import com.example.payment.entity.PaymentMethod;
import com.example.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByOrderId(Long orderId);
    
    List<Payment> findByOrderNumber(String orderNumber);
    
    List<Payment> findByUserId(String userId);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);
    
    List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status);
    
    Optional<Payment> findByTransactionId(String transactionId);
    
    Optional<Payment> findByPaymentReference(String paymentReference);
    
    List<Payment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Payment> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt >= :startDate")
    List<Payment> findRecentPaymentsByStatus(@Param("status") PaymentStatus status, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findUserPaymentsByStatus(@Param("userId") String userId, @Param("status") PaymentStatus status);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.userId = :userId AND p.status = :status")
    Long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") PaymentStatus status);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.userId = :userId AND p.status = :status")
    Double sumAmountByUserIdAndStatus(@Param("userId") String userId, @Param("status") PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.riskLevel = :riskLevel ORDER BY p.createdAt DESC")
    List<Payment> findHighRiskPayments(@Param("riskLevel") com.example.payment.entity.RiskLevel riskLevel);
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.createdAt >= :startDate ORDER BY p.createdAt DESC")
    List<Payment> findFailedPayments(@Param("startDate") LocalDateTime startDate);
}
