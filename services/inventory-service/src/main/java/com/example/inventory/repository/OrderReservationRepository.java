package com.example.inventory.repository;

import com.example.inventory.entity.OrderReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderReservationRepository extends JpaRepository<OrderReservation, Long> {
    
    List<OrderReservation> findByOrderId(String orderId);
    
    List<OrderReservation> findByOrderIdAndStatus(String orderId, OrderReservation.ReservationStatus status);
    
    List<OrderReservation> findByProductIdAndStatus(String productId, OrderReservation.ReservationStatus status);
    
    @Modifying
    @Query("UPDATE OrderReservation r SET r.status = 'RELEASED', r.releasedAt = CURRENT_TIMESTAMP WHERE r.orderId = :orderId AND r.status = 'RESERVED'")
    int releaseReservationsByOrderId(@Param("orderId") String orderId);
    
    @Modifying
    @Query("UPDATE OrderReservation r SET r.status = 'CONFIRMED', r.confirmedAt = CURRENT_TIMESTAMP WHERE r.orderId = :orderId AND r.status = 'RESERVED'")
    int confirmReservationsByOrderId(@Param("orderId") String orderId);
}
