@echo off
REM Verify test users trước khi chạy performance test

echo ========================================
echo VERIFY TEST USERS
echo ========================================
echo.

python verify_login_test.py http://localhost:8080 test_users.json

echo.
pause
