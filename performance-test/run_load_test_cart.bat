@echo off
REM Load Test - Add to Cart
REM Test khả năng chịu tải liên tục cho add to cart endpoint
REM 
REM Usage: run_load_test_cart.bat
REM 
REM Note: Cần cung cấp user IDs và product IDs
REM Ví dụ: python performance_test.py --test-type load-cart --duration 1 --rate 30 --user-ids "user1" "user2" --product-ids "prod1" "prod2"

echo ========================================
echo LOAD TEST - ADD TO CART
echo ========================================
echo.
echo NOTE: Cần cung cấp --user-ids và --product-ids
echo.
echo Ví dụ:
echo python performance_test.py --test-type load-cart --duration 1 --rate 30 --user-ids "user1" "user2" --product-ids "prod1" "prod2"
echo.

REM Nếu có user IDs và product IDs, uncomment và sửa:
REM python performance_test.py --test-type load-cart --duration 1 --rate 30 --user-ids "user1" "user2" --product-ids "prod1" "prod2" --output "load_test_cart_1min.json"
REM python performance_test.py --test-type load-cart --duration 3 --rate 30 --user-ids "user1" "user2" --product-ids "prod1" "prod2" --output "load_test_cart_3min.json"
REM python performance_test.py --test-type load-cart --duration 5 --rate 30 --user-ids "user1" "user2" --product-ids "prod1" "prod2" --output "load_test_cart_5min.json"

pause
