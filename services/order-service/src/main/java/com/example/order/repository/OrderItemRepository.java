package com.example.order.repository;

import com.example.order.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    interface TopProductStats {
        String getProductId();
        String getProductName();
        String getProductImage();
        Long getTotalQuantity();
        BigDecimal getTotalRevenue();
    }

    @Query("""
        select oi.productId as productId,
               oi.productName as productName,
               oi.productImage as productImage,
               sum(oi.quantity) as totalQuantity,
               sum(oi.totalPrice) as totalRevenue
        from OrderItem oi
        where oi.order.status = com.example.order.entity.OrderStatus.COMPLETED
        group by oi.productId, oi.productName, oi.productImage
        order by sum(oi.quantity) desc
    """)
    List<TopProductStats> findTopProducts(Pageable pageable);
}
