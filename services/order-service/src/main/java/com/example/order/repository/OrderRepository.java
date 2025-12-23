package com.example.order.repository;

import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC")
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.isFlaggedForReview = true ORDER BY o.createdAt DESC")
    Page<Order> findByIsFlaggedForReviewTrue(Pageable pageable);
    
    long countByStatus(OrderStatus status);
    long countByIsFlaggedForReviewTrue();
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    Page<Order> findByUserId(String userId, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByUserIdAndStatusIn(String userId, List<OrderStatus> statuses);

    // Custom query methods with fetch join for eager loading orderItems
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
    Optional<Order> findByIdWithOrderItems(Long id);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithOrderItems(String orderNumber);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems ORDER BY o.createdAt DESC")
    Page<Order> findAllWithOrderItems(Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.status = :status ORDER BY o.createdAt DESC")
    Page<Order> findByStatusWithOrderItems(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdWithOrderItems(String userId, Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.isFlaggedForReview = true ORDER BY o.createdAt DESC")
    Page<Order> findFlaggedWithOrderItems(Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.userId = :userId AND o.status IN :statuses")
    List<Order> findByUserIdAndStatusInWithOrderItems(String userId, List<OrderStatus> statuses);
    
    /**
     * Find orders by status, payment status, and created before a certain time
     * Used for cleanup of timeout orders
     */
    List<Order> findByStatusAndPaymentStatusAndCreatedAtBefore(
        OrderStatus status, 
        PaymentStatus paymentStatus, 
        LocalDateTime createdAt
    );
}


