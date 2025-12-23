@echo off
REM ===================================================
REM TRAIN RECOMMENDATION MODEL FROM DATABASE
REM ===================================================
echo.
echo ========================================
echo   TRAINING RECOMMENDATION MODEL
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
%PYTHON_CMD% -m pip show pandas >nul 2>&1
if %errorlevel% neq 0 (
    echo Installing basic dependencies...
    %PYTHON_CMD% -m pip install pandas numpy scikit-learn joblib pymysql
    if %errorlevel% neq 0 (
        echo [ERROR] Failed to install basic dependencies!
        pause
        exit /b 1
    )
)

%PYTHON_CMD% -m pip show surprise >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo Installing scikit-surprise (may require C++ Build Tools)...
    echo If this fails, you need to install Microsoft C++ Build Tools:
    echo   https://visualstudio.microsoft.com/visual-cpp-build-tools/
    echo.
    %PYTHON_CMD% -m pip install scikit-surprise
    if %errorlevel% neq 0 (
        echo.
        echo [WARNING] Failed to install scikit-surprise!
        echo.
        echo This requires Microsoft C++ Build Tools.
        echo Please install from: https://visualstudio.microsoft.com/visual-cpp-build-tools/
        echo.
        echo After installing, run this script again.
        pause
        exit /b 1
    )
) else (
    echo Dependencies are installed
)

echo.
echo [3/3] Training model...
echo.
echo This will:
echo   1. Load training data from training_data.csv
echo   2. Train SVD model on your reviews/ratings
echo   3. Save model to Buildd30_7/recommendation_model.pkl
echo   4. Update model_metadata.json
echo.
echo Press any key to start training...
pause >nul

%PYTHON_CMD% train_model.py

echo.
pause

