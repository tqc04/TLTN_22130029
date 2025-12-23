# PowerShell script để chạy Stress Test - Tìm kiếm sản phẩm
# Chạy các test với 300, 500, 1000 requests

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "STRESS TEST - TÌM KIẾM SẢN PHẨM" -ForegroundColor Cyan
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
Write-Host "Bắt đầu Stress Test..." -ForegroundColor Green
Write-Host ""

# Test với 300 requests
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Test 1: 300 requests" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
python performance_test.py --test-type stress-search --requests 300 --output "stress_test_300.json"
Write-Host ""

# Test với 500 requests
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Test 2: 500 requests" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
python performance_test.py --test-type stress-search --requests 500 --output "stress_test_500.json"
Write-Host ""

# Test với 1000 requests
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Test 3: 1000 requests" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
python performance_test.py --test-type stress-search --requests 1000 --output "stress_test_1000.json"
Write-Host ""

Write-Host "========================================" -ForegroundColor Green
Write-Host "TẤT CẢ CÁC TEST ĐÃ HOÀN THÀNH" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Kết quả đã được lưu vào:" -ForegroundColor Cyan
Write-Host "  - stress_test_300.json" -ForegroundColor White
Write-Host "  - stress_test_500.json" -ForegroundColor White
Write-Host "  - stress_test_1000.json" -ForegroundColor White

