@echo off
REM Stress Test - User Login
REM Test khả năng chịu tải đột biến cho login endpoint

echo ========================================
echo STRESS TEST - USER LOGIN
echo ========================================
echo.

python performance_test.py --test-type stress-login --requests 300 --output "stress_test_login_300.json"
echo.
python performance_test.py --test-type stress-login --requests 500 --output "stress_test_login_500.json"
echo.
python performance_test.py --test-type stress-login --requests 1000 --output "stress_test_login_1000.json"

echo.
echo ========================================
echo STRESS TEST LOGIN COMPLETED
echo ========================================
pause
