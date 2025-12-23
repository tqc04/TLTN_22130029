@echo off
REM Batch script để chạy Stress Test - Tìm kiếm sản phẩm
REM Chạy các test với 300, 500, 1000 requests

echo ========================================
echo STRESS TEST - TIM KIEM SAN PHAM
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
echo Bat dau Stress Test...
echo.

REM Test với 300 requests
echo ----------------------------------------
echo Test 1: 300 requests
echo ----------------------------------------
python performance_test.py --test-type stress-search --requests 300 --output "stress_test_300.json"
echo.

REM Test với 500 requests
echo ----------------------------------------
echo Test 2: 500 requests
echo ----------------------------------------
python performance_test.py --test-type stress-search --requests 500 --output "stress_test_500.json"
echo.

REM Test với 1000 requests
echo ----------------------------------------
echo Test 3: 1000 requests
echo ----------------------------------------
python performance_test.py --test-type stress-search --requests 1000 --output "stress_test_1000.json"
echo.

echo ========================================
echo TAT CA CAC TEST DA HOAN THANH
echo ========================================
echo.
echo Ket qua da duoc luu vao:
echo   - stress_test_300.json
echo   - stress_test_500.json
echo   - stress_test_1000.json
echo.

pause

