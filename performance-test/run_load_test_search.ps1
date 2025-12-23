# PowerShell script để chạy Load Test - Tìm kiếm sản phẩm
# Chạy các test với 1, 3, 5 phút

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "LOAD TEST - TÌM KIẾM SẢN PHẨM" -ForegroundColor Cyan
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
Write-Host "Bắt đầu Load Test..." -ForegroundColor Green
Write-Host ""

# Test trong 1 phút
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Test 1: 1 phút (50 requests/giây)" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
python performance_test.py --test-type load-search --duration 1 --rate 50 --output "load_test_search_1min.json"
Write-Host ""

# Test trong 3 phút
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Test 2: 3 phút (50 requests/giây)" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
python performance_test.py --test-type load-search --duration 3 --rate 50 --output "load_test_search_3min.json"
Write-Host ""

# Test trong 5 phút
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Test 3: 5 phút (50 requests/giây)" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
python performance_test.py --test-type load-search --duration 5 --rate 50 --output "load_test_search_5min.json"
Write-Host ""

Write-Host "========================================" -ForegroundColor Green
Write-Host "TẤT CẢ CÁC TEST ĐÃ HOÀN THÀNH" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Kết quả đã được lưu vào:" -ForegroundColor Cyan
Write-Host "  - load_test_search_1min.json" -ForegroundColor White
Write-Host "  - load_test_search_3min.json" -ForegroundColor White
Write-Host "  - load_test_search_5min.json" -ForegroundColor White

