@echo off
REM ============================================
REM EXTRACT TRAINING DATA
REM ============================================
REM This script sets environment variables and runs extract_training_data.py
REM ============================================

echo.
echo ============================================
echo  EXTRACTING TRAINING DATA
echo ============================================
echo.

cd /d "%~dp0"

REM Set default database credentials if not already set
if not defined DB_HOST set DB_HOST=localhost
if not defined DB_PORT set DB_PORT=3306
if not defined DB_USER set DB_USER=root
if not defined DB_PASSWORD set DB_PASSWORD=123456

echo Database Configuration:
echo   Host: %DB_HOST%
echo   Port: %DB_PORT%
echo   User: %DB_USER%
echo   Password: ********
echo.

REM Set environment variables for this session
set DB_HOST=%DB_HOST%
set DB_PORT=%DB_PORT%
set DB_USER=%DB_USER%
set DB_PASSWORD=%DB_PASSWORD%

echo Running extract_training_data.py...
echo.

python extract_training_data.py

if errorlevel 1 (
    echo.
    echo [ERROR] Extraction failed!
    echo.
    echo Make sure:
    echo   1. MySQL is running
    echo   2. Databases exist: review_service_db, order_db, ecommerce_db
    echo   3. Database credentials are correct
    echo.
    echo To configure, run: .\SETUP.bat
    echo.
    pause
    exit /b 1
)

echo.
echo ============================================
echo  ✅ EXTRACTION COMPLETE!
echo ============================================
echo.
echo Next step: Run .\TRAIN.bat to train the model
echo.
pause

