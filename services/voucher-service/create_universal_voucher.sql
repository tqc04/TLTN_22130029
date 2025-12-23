-- SQL script to create a universal voucher that can be used for all orders
-- Voucher này có thể dùng cho tất cả đơn hàng (min_order_amount = 0)

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
    usage_limit,
    usage_limit_per_user,
    usage_count,
    is_active,
    is_public,
    applicable_to,
    free_shipping,
    created_at,
    updated_at
) VALUES (
    'UNIVERSAL10',
    'Giảm giá 10% cho mọi đơn hàng',
    'Voucher giảm 10% cho tất cả đơn hàng, không giới hạn giá trị đơn hàng tối thiểu. Áp dụng cho tất cả sản phẩm. Hạn sử dụng 1 năm.',
    'PERCENTAGE',
    10.00,
    0.00,  -- Không giới hạn giá trị đơn hàng tối thiểu
    50000.00,  -- Giảm tối đa 50,000 VNĐ
    NOW(),
    DATE_ADD(NOW(), INTERVAL 1 YEAR),
    10000,  -- Giới hạn 10,000 lượt sử dụng
    5,  -- Mỗi user dùng tối đa 5 lần
    0,
    1,  -- is_active = TRUE
    1,  -- is_public = TRUE
    'ALL',  -- Áp dụng cho tất cả sản phẩm
    0,  -- Không miễn phí ship
    NOW(),
    NOW()
);

