@echo off
REM ===================================================
REM INSTALL DEPENDENCIES FOR ML TRAINING
REM ===================================================
echo.
echo ========================================
echo   INSTALLING DEPENDENCIES
echo ========================================
echo.

cd /d %~dp0

REM Check if Python is installed
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python not found!
    echo Please install Python 3.8+ from https://www.python.org/
    pause
    exit /b 1
)

echo [1/4] Installing basic dependencies...
python -m pip install --upgrade pip
python -m pip install pymysql pandas numpy scikit-learn joblib

if %errorlevel% neq 0 (
    echo [ERROR] Failed to install basic dependencies!
    pause
    exit /b 1
)

echo.
echo [2/4] Installing scikit-surprise...
echo.
echo NOTE: scikit-surprise requires Microsoft C++ Build Tools
echo If installation fails, please install:
echo   https://visualstudio.microsoft.com/visual-cpp-build-tools/
echo.
echo Installing scikit-surprise...
python -m pip install scikit-surprise

if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo   INSTALLATION FAILED
    echo ========================================
    echo.
    echo scikit-surprise requires Microsoft C++ Build Tools.
    echo.
    echo Please:
    echo   1. Download and install from:
    echo      https://visualstudio.microsoft.com/visual-cpp-build-tools/
    echo   2. During installation, select:
    echo      - "Desktop development with C++"
    echo   3. Restart your computer
    echo   4. Run this script again
    echo.
    pause
    exit /b 1
)

echo.
echo [3/4] Verifying installation...
python -c "import pymysql, pandas, numpy, sklearn, joblib, surprise; print('✅ All dependencies installed successfully!')"

if %errorlevel% neq 0 (
    echo [ERROR] Some dependencies are missing!
    pause
    exit /b 1
)

echo.
echo [4/4] Complete!
echo.
echo ========================================
echo   ALL DEPENDENCIES INSTALLED
echo ========================================
echo.
echo You can now run:
echo   python extract_training_data.py
echo   python train_model.py
echo.
pause

