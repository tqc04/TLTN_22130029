@echo off
REM ===================================================
REM SETUP DATABASE CONFIGURATION
REM ===================================================
echo.
echo ========================================
echo   DATABASE CONFIGURATION SETUP
echo ========================================
echo.

cd /d %~dp0

REM Check if environment variables already exist
if defined DB_HOST (
    echo Using existing environment variables:
    echo   DB_HOST=%DB_HOST%
    echo   DB_PORT=%DB_PORT%
    echo   DB_USER=%DB_USER%
    echo   DB_NAME=%DB_NAME%
    echo.
    echo To reconfigure, clear environment variables first.
    echo.
    pause
    exit /b 0
)

REM Try to read from .env file if exists
if exist "..\..\.env" (
    echo Found .env file, reading configuration...
    for /f "tokens=1,2 delims==" %%a in ('findstr /i "DB_" "..\..\.env"') do (
        if /i "%%a"=="DB_USERNAME" set DB_USER=%%b
        if /i "%%a"=="DB_PASSWORD" set DB_PASSWORD=%%b
    )
    for /f "tokens=1,2 delims=:" %%a in ('findstr /i "DB_URL" "..\..\.env"') do (
        REM Extract host and port from JDBC URL
    )
)

echo This script will help you configure database connection.
echo.
echo Default values (press Enter to use):
echo   Host: localhost
echo   Port: 3306
echo   User: root
echo   Password: 123456
echo.
echo Please provide your MySQL database credentials:
echo.

set /p DB_HOST="Database Host [localhost]: "
if "%DB_HOST%"=="" set DB_HOST=localhost

set /p DB_PORT="Database Port [3306]: "
if "%DB_PORT%"=="" set DB_PORT=3306

set /p DB_USER="Database User [root]: "
if "%DB_USER%"=="" set DB_USER=root

if not defined DB_PASSWORD (
    set /p DB_PASSWORD="Database Password [123456]: "
    if "%DB_PASSWORD%"=="" set DB_PASSWORD=123456
)

REM Note: We connect to multiple databases (review_service_db, order_db, ecommerce_db)
REM DB_NAME is not used, but we keep it for compatibility
set DB_NAME=ecommerce

echo.
echo Setting environment variables for this session...
set DB_HOST=%DB_HOST%
set DB_PORT=%DB_PORT%
set DB_USER=%DB_USER%
set DB_PASSWORD=%DB_PASSWORD%
set DB_NAME=%DB_NAME%

echo.
echo ========================================
echo   CONFIGURATION SAVED
echo ========================================
echo.
echo Database Host: %DB_HOST%
echo Database Port: %DB_PORT%
echo Database User: %DB_USER%
echo Database Password: ********
echo.
echo NOTE: These settings are only for this session.
echo The script will connect to these databases:
echo   - review_service_db (for product reviews)
echo   - order_db (for purchase data)
echo   - ecommerce_db (for user behaviors)
echo.
echo To make settings permanent, create a .env file or set system environment variables.
echo.
echo You can now run:
echo   python extract_training_data.py
echo   or
echo   .\RUN_ALL.bat
echo.
pause

