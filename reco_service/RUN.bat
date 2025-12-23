@echo off
REM ===================================================
REM RUN PYTHON ML RECOMMENDATION SERVICE
REM ===================================================
echo.
echo ========================================
echo   STARTING PYTHON ML SERVICE...
echo ========================================
echo.

cd /d %~dp0

REM Check if Python is installed
python --version >nul 2>&1
if %errorlevel% neq 0 (
    python3 --version >nul 2>&1
    if %errorlevel% neq 0 (
        echo [ERROR] Python not found!
        echo Please install Python 3.8+ from https://www.python.org/
        pause
        exit /b 1
    ) else (
        set PYTHON_CMD=python3
    )
) else (
    set PYTHON_CMD=python
)

echo [1/3] Checking Python installation...
%PYTHON_CMD% --version

echo.
echo [2/3] Checking dependencies...
%PYTHON_CMD% -m pip show fastapi >nul 2>&1
if %errorlevel% neq 0 (
    echo Installing dependencies...
    %PYTHON_CMD% -m pip install -r requirements.txt
    if %errorlevel% neq 0 (
        echo [ERROR] Failed to install dependencies!
        pause
        exit /b 1
    )
) else (
    echo Dependencies are installed
)

echo.
echo [3/3] Starting ML Service...
echo.
echo Service will be available at:
echo   http://localhost:8000
echo   http://localhost:8000/health
echo   http://localhost:8000/docs
echo.
echo Press Ctrl+C to stop the service
echo.

set PORT=8000
%PYTHON_CMD% app.py

pause

