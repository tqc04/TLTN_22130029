@echo off
REM Stress Test - Add to Cart
REM Test khả năng chịu tải đột biến cho add to cart endpoint
REM 
REM Usage: run_stress_test_cart.bat
REM 
REM Note: Cần cung cấp user IDs và product IDs
REM Ví dụ: python performance_test.py --test-type stress-cart --requests 300 --user-ids "user1" "user2" --product-ids "prod1" "prod2"

echo ========================================
echo STRESS TEST - ADD TO CART
echo ========================================
echo.
echo NOTE: Cần cung cấp --user-ids và --product-ids
echo.
echo Ví dụ:
echo python performance_test.py --test-type stress-cart --requests 300 --user-ids "user1" "user2" --product-ids "prod1" "prod2"
echo.

REM Nếu có user IDs và product IDs, uncomment và sửa:
REM python performance_test.py --test-type stress-cart --requests 300 --user-ids "user1" "user2" --product-ids "prod1" "prod2" --output "stress_test_cart_300.json"
REM python performance_test.py --test-type stress-cart --requests 500 --user-ids "user1" "user2" --product-ids "prod1" "prod2" --output "stress_test_cart_500.json"
REM python performance_test.py --test-type stress-cart --requests 1000 --user-ids "user1" "user2" --product-ids "prod1" "prod2" --output "stress_test_cart_1000.json"

pause
