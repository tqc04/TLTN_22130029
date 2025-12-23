# PowerShell script để chạy tất cả các test
# Bao gồm: Stress Test và Load Test cho cả tìm kiếm và xem chi tiết

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CHẠY TẤT CẢ CÁC PERFORMANCE TEST" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra Python
if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    Write-Host "Lỗi: Python chưa được cài đặt hoặc không có trong PATH" -ForegroundColor Red
    exit 1
}

# Kiểm tra file requirements
if (Test-Path "requirements.txt") {
    Write-Host "Đang kiểm tra dependencies..." -ForegroundColor Yellow
    pip install -q -r requirements.txt
}

Write-Host ""
Write-Host "Bắt đầu chạy tất cả các test..." -ForegroundColor Green
Write-Host ""

# ============================================
# STRESS TEST - TÌM KIẾM SẢN PHẨM
# ============================================
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "PHẦN 1: STRESS TEST - TÌM KIẾM SẢN PHẨM" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

Write-Host "Test 1.1: 300 requests" -ForegroundColor Yellow
python performance_test.py --test-type stress-search --requests 300 --output "stress_test_search_300.json"
Write-Host ""

Write-Host "Test 1.2: 500 requests" -ForegroundColor Yellow
python performance_test.py --test-type stress-search --requests 500 --output "stress_test_search_500.json"
Write-Host ""

Write-Host "Test 1.3: 1000 requests" -ForegroundColor Yellow
python performance_test.py --test-type stress-search --requests 1000 --output "stress_test_search_1000.json"
Write-Host ""

# ============================================
# LOAD TEST - TÌM KIẾM SẢN PHẨM
# ============================================
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "PHẦN 2: LOAD TEST - TÌM KIẾM SẢN PHẨM" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

Write-Host "Test 2.1: 1 phút (50 requests/giây)" -ForegroundColor Yellow
python performance_test.py --test-type load-search --duration 1 --rate 50 --output "load_test_search_1min.json"
Write-Host ""

Write-Host "Test 2.2: 3 phút (50 requests/giây)" -ForegroundColor Yellow
python performance_test.py --test-type load-search --duration 3 --rate 50 --output "load_test_search_3min.json"
Write-Host ""

Write-Host "Test 2.3: 5 phút (50 requests/giây)" -ForegroundColor Yellow
python performance_test.py --test-type load-search --duration 5 --rate 50 --output "load_test_search_5min.json"
Write-Host ""

# ============================================
# LOAD TEST - XEM CHI TIẾT SẢN PHẨM
# ============================================
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "PHẦN 3: LOAD TEST - XEM CHI TIẾT SẢN PHẨM" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

Write-Host "Test 3.1: 1 phút (60 requests/giây)" -ForegroundColor Yellow
python performance_test.py --test-type load-detail --duration 1 --rate 60 --output "load_test_detail_1min.json"
Write-Host ""

Write-Host "Test 3.2: 3 phút (60 requests/giây)" -ForegroundColor Yellow
python performance_test.py --test-type load-detail --duration 3 --rate 60 --output "load_test_detail_3min.json"
Write-Host ""

Write-Host "Test 3.3: 5 phút (60 requests/giây)" -ForegroundColor Yellow
python performance_test.py --test-type load-detail --duration 5 --rate 60 --output "load_test_detail_5min.json"
Write-Host ""

# ============================================
# TỔNG KẾT
# ============================================
Write-Host "========================================" -ForegroundColor Green
Write-Host "TẤT CẢ CÁC TEST ĐÃ HOÀN THÀNH" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Kết quả đã được lưu vào các file JSON:" -ForegroundColor Cyan
Write-Host ""
Write-Host "Stress Test - Tìm kiếm:" -ForegroundColor White
Write-Host "  - stress_test_search_300.json" -ForegroundColor Gray
Write-Host "  - stress_test_search_500.json" -ForegroundColor Gray
Write-Host "  - stress_test_search_1000.json" -ForegroundColor Gray
Write-Host ""
Write-Host "Load Test - Tìm kiếm:" -ForegroundColor White
Write-Host "  - load_test_search_1min.json" -ForegroundColor Gray
Write-Host "  - load_test_search_3min.json" -ForegroundColor Gray
Write-Host "  - load_test_search_5min.json" -ForegroundColor Gray
Write-Host ""
Write-Host "Load Test - Xem chi tiết:" -ForegroundColor White
Write-Host "  - load_test_detail_1min.json" -ForegroundColor Gray
Write-Host "  - load_test_detail_3min.json" -ForegroundColor Gray
Write-Host "  - load_test_detail_5min.json" -ForegroundColor Gray
Write-Host ""

