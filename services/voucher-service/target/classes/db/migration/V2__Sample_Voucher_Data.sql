-- V2__Sample_Voucher_Data.sql
-- Sample data for testing voucher system

-- Insert sample vouchers with different configurations
INSERT INTO vouchers (code, name, description, type, value, min_order_amount, max_discount_amount, usage_limit, usage_limit_per_user, is_active, is_public, applicable_to, created_at, updated_at) VALUES
-- Percentage vouchers
('WELCOME10', 'Welcome Discount 10%', 'Get 10% off on your first order', 'PERCENTAGE', 10.00, 500000, 100000, 1000, 1, TRUE, TRUE, 'ALL', NOW(), NOW()),
('STUDENT15', 'Student Discount 15%', '15% discount for verified students', 'PERCENTAGE', 15.00, 300000, 200000, 500, 3, TRUE, FALSE, 'ALL', NOW(), NOW()),
('FLASH30', 'Flash Sale 30%', '30% off on selected items - Limited time', 'PERCENTAGE', 30.00, 1000000, 500000, 100, 1, TRUE, TRUE, 'ALL', NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY)),
('VIP20', 'VIP Member 20%', 'Exclusive 20% discount for VIP members', 'PERCENTAGE', 20.00, 2000000, 1000000, 200, 5, TRUE, FALSE, 'ALL', NOW(), NOW()),

-- Fixed amount vouchers
('NEWCUSTOMER', 'New Customer Special', '500,000 VND off for new customers', 'FIXED_AMOUNT', 500000, 2000000, NULL, 200, 1, TRUE, TRUE, 'ALL', NOW(), NOW()),
('BIRTHDAY', 'Birthday Special', '1,000,000 VND birthday discount', 'FIXED_AMOUNT', 1000000, 3000000, NULL, 1000, 1, TRUE, FALSE, 'ALL', NOW(), NOW()),
('FIRSTORDER', 'First Order Discount', '200,000 VND off your first purchase', 'FIXED_AMOUNT', 200000, 1000000, NULL, 5000, 1, TRUE, TRUE, 'ALL', NOW(), NOW()),
('TECH50', 'Tech Lovers Discount', '500,000 VND off electronics', 'FIXED_AMOUNT', 500000, 5000000, NULL, 300, 2, TRUE, TRUE, 'CATEGORY', NOW(), NOW()),

-- Limited time offers
('BLACKFRIDAY', 'Black Friday 50%', '50% off everything - Black Friday only', 'PERCENTAGE', 50.00, 500000, 2000000, 50, 1, TRUE, TRUE, 'ALL', NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY)),
('NEWYEAR25', 'New Year Special', '25% off to celebrate new year', 'PERCENTAGE', 25.00, 1000000, 800000, 1000, 2, TRUE, TRUE, 'ALL', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY)),

-- Category specific vouchers
('LAPTOP20', 'Laptop Discount 20%', '20% off all laptops', 'PERCENTAGE', 20.00, 10000000, 2000000, 100, 1, TRUE, TRUE, 'CATEGORY', NOW(), NOW()),
('PHONE15', 'Phone Discount 15%', '15% off smartphones', 'PERCENTAGE', 15.00, 5000000, 1000000, 200, 2, TRUE, TRUE, 'CATEGORY', NOW(), NOW()),

-- Brand specific vouchers
('SAMSUNG10', 'Samsung Special 10%', '10% off Samsung products', 'PERCENTAGE', 10.00, 2000000, 500000, 300, 3, TRUE, TRUE, 'BRAND', NOW(), NOW()),
('APPLE15', 'Apple Discount 15%', '15% off Apple products', 'PERCENTAGE', 15.00, 5000000, 1500000, 150, 2, TRUE, TRUE, 'BRAND', NOW(), NOW());

-- Insert sample voucher usage records
INSERT INTO voucher_usages (voucher_id, voucher_code, user_id, order_id, order_number, original_amount, discount_amount, final_amount, used_at, created_at) VALUES
(1, 'WELCOME10', 1, 1001, 'ORD-001', 1000000.00, 100000.00, 900000.00, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 'WELCOME10', 2, 1002, 'ORD-002', 1500000.00, 150000.00, 1350000.00, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 'STUDENT15', 3, 1003, 'ORD-003', 800000.00, 120000.00, 680000.00, NOW(), NOW());
