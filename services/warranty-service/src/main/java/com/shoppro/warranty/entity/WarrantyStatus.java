package com.shoppro.warranty.entity;

public enum WarrantyStatus {
    PENDING,        // Yêu cầu mới, đang chờ xử lý
    APPROVED,       // Đã duyệt, đang chờ nhận sản phẩm
    RECEIVED,       // Đã nhận sản phẩm từ khách hàng
    IN_PROGRESS,    // Đang sửa chữa/kiểm tra
    COMPLETED,      // Hoàn thành bảo hành
    REJECTED,       // Từ chối bảo hành
    CANCELLED       // Khách hàng hủy yêu cầu
}
