@echo off
REM Batch script để chạy Load Test - Xem chi tiết sản phẩm
REM Chạy các test với 1, 3, 5 phút

echo ========================================
echo LOAD TEST - XEM CHI TIET SAN PHAM
echo ========================================
echo.

REM Kiểm tra Python
python --version >nul 2>&1
if errorlevel 1 (
    echo Loi: Python chua duoc cai dat hoac khong co trong PATH
    exit /b 1
)

REM Kiểm tra file requirements
if exist requirements.txt (
    echo Dang kiem tra dependencies...
    pip install -q -r requirements.txt
)

echo.
echo Bat dau Load Test...
echo.

REM Test trong 1 phút
echo ----------------------------------------
echo Test 1: 1 phut (60 requests/giay)
echo ----------------------------------------
python performance_test.py --test-type load-detail --duration 1 --rate 60 --output "load_test_detail_1min.json"
echo.

REM Test trong 3 phút
echo ----------------------------------------
echo Test 2: 3 phut (60 requests/giay)
echo ----------------------------------------
python performance_test.py --test-type load-detail --duration 3 --rate 60 --output "load_test_detail_3min.json"
echo.

REM Test trong 5 phút
echo ----------------------------------------
echo Test 3: 5 phut (60 requests/giay)
echo ----------------------------------------
python performance_test.py --test-type load-detail --duration 5 --rate 60 --output "load_test_detail_5min.json"
echo.

echo ========================================
echo TAT CA CAC TEST DA HOAN THANH
echo ========================================
echo.
echo Ket qua da duoc luu vao:
echo   - load_test_detail_1min.json
echo   - load_test_detail_3min.json
echo   - load_test_detail_5min.json
echo.

pause

