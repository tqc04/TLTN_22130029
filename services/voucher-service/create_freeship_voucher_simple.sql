-- SQL đơn giản để tạo mã Free Ship 50k
-- Chạy trực tiếp trong MySQL/MariaDB

INSERT INTO vouchers (
    code,
    name,
    description,
    type,
    value,
    min_order_amount,
    max_discount_amount,
    start_date,
    end_date,
    usage_count,
    is_active,
    is_public,
    applicable_to,
    free_shipping,
    created_at,
    updated_at
) VALUES (
    'FREESHIP50K',
    'Free Ship 50k',
    'Miễn phí vận chuyển 50,000 VNĐ cho đơn hàng từ 500,000 VNĐ. Hạn sử dụng 1 năm.',
    'FIXED_AMOUNT',
    50000,
    500000,
    50000,
    NOW(),
    DATE_ADD(NOW(), INTERVAL 1 YEAR),
    0,
    TRUE,
    TRUE,
    'ALL',
    TRUE,
    NOW(),
    NOW()
);

