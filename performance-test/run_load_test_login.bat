@echo off
REM Load Test - User Login
REM Test khả năng chịu tải liên tục cho login endpoint

echo ========================================
echo LOAD TEST - USER LOGIN
echo ========================================
echo.

echo Test 1: 1 phút (20 requests/giây)
python performance_test.py --test-type load-login --duration 1 --rate 20 --output "load_test_login_1min.json"
echo.

echo Test 2: 3 phút (20 requests/giây)
python performance_test.py --test-type load-login --duration 3 --rate 20 --output "load_test_login_3min.json"
echo.

echo Test 3: 5 phút (20 requests/giây)
python performance_test.py --test-type load-login --duration 5 --rate 20 --output "load_test_login_5min.json"

echo.
echo ========================================
echo LOAD TEST LOGIN COMPLETED
echo ========================================
pause
