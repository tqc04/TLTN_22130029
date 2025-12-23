@echo off
REM Quick Start - Chạy test nhanh trong 5 phút

echo ========================================
echo   PERFORMANCE TEST - QUICK START
echo ========================================
echo.

REM Kiểm tra Python
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python chua duoc cai dat!
    echo Vui long cai Python tu: https://www.python.org/downloads/
    pause
    exit /b 1
)

REM Cài dependencies
echo [1/3] Cai dat dependencies...
pip install -q -r requirements.txt
if errorlevel 1 (
    echo [ERROR] Khong the cai dat dependencies!
    pause
    exit /b 1
)
echo      OK!
echo.

REM Kiểm tra backend
echo [2/3] Kiem tra backend...
curl -s http://localhost:8080/api/products?page=0^&size=1 >nul 2>&1
if errorlevel 1 (
    echo [WARNING] Backend chua chay hoac khong ket noi duoc!
    echo Vui long chay backend truoc: cd ..\services ^&^& .\RUN.bat
    echo.
    echo Ban co muon tiep tuc test khong? (Y/N)
    set /p continue=
    if /i not "%continue%"=="Y" exit /b 1
)
echo      OK!
echo.

REM Chạy test
echo [3/3] Chay test...
echo.
echo ========================================
echo   STRESS TEST - 300 REQUESTS
echo ========================================
echo.
python performance_test.py --test-type stress-search --requests 300 --output "quick_stress_test.json"
echo.

echo ========================================
echo   LOAD TEST - 1 PHUT (50 req/s)
echo ========================================
echo.
python performance_test.py --test-type load-search --duration 1 --rate 50 --output "quick_load_test.json"
echo.

echo ========================================
echo   HOAN THANH!
echo ========================================
echo.
echo Ket qua da duoc luu vao:
echo   - quick_stress_test.json
echo   - quick_load_test.json
echo.
echo De chay tat ca test, dung: .\run_all_tests.ps1
echo.
pause

