-- V1__Create_Voucher_Tables.sql
-- Migration for Voucher System

-- Create vouchers table
CREATE TABLE vouchers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    type ENUM('PERCENTAGE', 'FIXED_AMOUNT') NOT NULL,
    value DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2),
    max_discount_amount DECIMAL(10,2),
    start_date DATETIME,
    end_date DATETIME,
    usage_limit INT,
    usage_count INT DEFAULT 0,
    usage_limit_per_user INT,
    is_active BOOLEAN DEFAULT TRUE,
    is_public BOOLEAN DEFAULT TRUE,
    applicable_to VARCHAR(50), -- ALL, CATEGORY, BRAND, PRODUCT
    applicable_items TEXT, -- JSON array of IDs
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_voucher_code (code),
    INDEX idx_voucher_active (is_active),
    INDEX idx_voucher_public (is_public),
    INDEX idx_voucher_dates (start_date, end_date),
    INDEX idx_voucher_usage (usage_count, usage_limit)
);

-- Create voucher_usages table
CREATE TABLE voucher_usages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    voucher_id BIGINT NOT NULL,
    voucher_code VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_number VARCHAR(100) NOT NULL,
    original_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL,
    final_amount DECIMAL(10,2) NOT NULL,
    used_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_voucher_usage_voucher (voucher_id),
    INDEX idx_voucher_usage_user (user_id),
    INDEX idx_voucher_usage_order (order_id),
    INDEX idx_voucher_usage_order_number (order_number),
    INDEX idx_voucher_usage_date (used_at),
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE
);

-- Insert sample vouchers for testing
INSERT INTO vouchers (code, name, description, type, value, min_order_amount, usage_limit, usage_limit_per_user, is_active, is_public, created_at, updated_at) VALUES
('WELCOME10', 'Welcome Discount 10%', 'Get 10% off on your first order', 'PERCENTAGE', 10.00, 500000, 1000, 1, TRUE, TRUE, NOW(), NOW()),
('SAVE20', 'Save 20%', 'Get 20% discount on orders over 1 million VND', 'PERCENTAGE', 20.00, 1000000, 500, 3, TRUE, TRUE, NOW(), NOW()),
('NEWCUSTOMER', 'New Customer Special', '500,000 VND off for new customers', 'FIXED_AMOUNT', 500000, 2000000, 200, 1, TRUE, TRUE, NOW(), NOW()),
('STUDENT15', 'Student Discount', '15% discount for students', 'PERCENTAGE', 15.00, 300000, 1000, 5, TRUE, FALSE, NOW(), NOW()),
('FLASH50', 'Flash Sale 50%', '50% off on selected items - Limited time', 'PERCENTAGE', 50.00, 100000, 100, 1, TRUE, TRUE, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY));
