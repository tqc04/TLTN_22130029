@echo off
REM ============================================
REM CHECK MODEL STATUS
REM ============================================
REM This script checks if model is trained from Amazon data or your database
REM ============================================

echo.
echo ============================================
echo  CHECKING MODEL STATUS
echo ============================================
echo.

cd /d "%~dp0"

set "MODEL_DIR=..\.."
set "METADATA_FILE=%MODEL_DIR%\model_metadata.json"

if not exist "%METADATA_FILE%" (
    echo [ERROR] model_metadata.json not found!
    echo.
    echo Model has not been trained yet.
    echo Please run: .\RUN_ALL.bat
    echo.
    pause
    exit /b 1
)

echo Checking model metadata...
echo.

REM Check if metadata contains Amazon
findstr /i "Amazon" "%METADATA_FILE%" >nul 2>&1
if errorlevel 1 (
    echo [OK] Model appears to be trained from YOUR database
    echo.
) else (
    echo [WARNING] Model is trained from AMAZON DATA!
    echo.
    echo Current model source: Amazon Electronics Ratings
    echo.
    echo This model will NOT work correctly with your database because:
    echo   - User IDs from Amazon don't match your user IDs
    echo   - Product IDs from Amazon don't match your product IDs
    echo   - Model learned from Amazon users' behavior, not your users
    echo.
    echo SOLUTION: Train model from YOUR database
    echo.
    echo Steps:
    echo   1. Run: .\EXTRACT.bat (extract data from your databases)
    echo   2. Run: .\TRAIN.bat (train model from your data)
    echo   3. Restart Python ML Service
    echo.
)

echo Model metadata:
type "%METADATA_FILE%"
echo.
pause

