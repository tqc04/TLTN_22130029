package com.example.order.repository;

import com.example.order.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    // Đã bỏ isDefault, không cần method này nữa
} 