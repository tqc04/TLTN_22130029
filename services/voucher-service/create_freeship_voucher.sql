-- Tạo mã voucher Free Ship 50k cho đơn hàng trên 500,000 VNĐ
-- Hạn sử dụng: 1 năm từ ngày tạo

-- Kiểm tra và thêm cột free_shipping nếu chưa có
ALTER TABLE vouchers 
ADD COLUMN IF NOT EXISTS free_shipping BOOLEAN DEFAULT FALSE;

-- Tạo mã voucher Free Ship
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
    usage_count,
    usage_limit_per_user,
    is_active,
    is_public,
    applicable_to,
    applicable_items,
    free_shipping,
    image_url,
    created_at,
    updated_at
) VALUES (
    'FREESHIP50K',  -- Mã voucher
    'Free Ship 50k',  -- Tên voucher
    'Miễn phí vận chuyển 50,000 VNĐ cho đơn hàng từ 500,000 VNĐ. Áp dụng cho tất cả sản phẩm. Hạn sử dụng 1 năm.',  -- Mô tả
    'FIXED_AMOUNT',  -- Loại: giảm giá cố định
    50000,  -- Giá trị: 50,000 VNĐ
    500000,  -- Đơn hàng tối thiểu: 500,000 VNĐ
    50000,  -- Giảm giá tối đa: 50,000 VNĐ (bằng value vì là fixed amount)
    NOW(),  -- Ngày bắt đầu: hôm nay
    DATE_ADD(NOW(), INTERVAL 1 YEAR),  -- Ngày kết thúc: 1 năm sau
    NULL,  -- Không giới hạn số lần sử dụng tổng thể
    0,  -- Số lần đã sử dụng: 0
    NULL,  -- Không giới hạn số lần sử dụng mỗi user
    TRUE,  -- Đang hoạt động
    TRUE,  -- Công khai (user có thể thấy)
    'ALL',  -- Áp dụng cho tất cả sản phẩm
    NULL,  -- Không có danh sách sản phẩm cụ thể
    TRUE,  -- Free shipping: TRUE
    NULL,  -- Không có hình ảnh
    NOW(),  -- Ngày tạo
    NOW()   -- Ngày cập nhật
);

-- Kiểm tra kết quả
SELECT 
    id,
    code,
    name,
    description,
    type,
    value,
    min_order_amount,
    free_shipping,
    start_date,
    end_date,
    is_active,
    is_public,
    created_at
FROM vouchers
WHERE code = 'FREESHIP50K';

