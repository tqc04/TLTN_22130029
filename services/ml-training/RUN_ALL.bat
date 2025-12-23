@echo off
REM ============================================
REM RUN ALL - Train Recommendation Model
REM ============================================
REM This script runs the complete training pipeline:
REM 1. Extract training data from databases
REM 2. Train the model
REM 3. Copy model files to reco_service
REM ============================================

echo.
echo ============================================
echo  TRAINING RECOMMENDATION MODEL
echo ============================================
echo.

cd /d "%~dp0"

REM Check Python
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python is not installed or not in PATH
    echo Please install Python 3.8+ and try again
    pause
    exit /b 1
)

echo [1/3] Extracting training data from databases...
echo.
python extract_training_data.py
if errorlevel 1 (
    echo.
    echo [ERROR] Failed to extract training data
    pause
    exit /b 1
)

echo.
echo [2/3] Training recommendation model...
echo.
python train_model.py
if errorlevel 1 (
    echo.
    echo [ERROR] Failed to train model
    pause
    exit /b 1
)

echo.
echo [3/3] Copying model files to reco_service...
echo.

REM Copy model files to reco_service directory
set "RECO_SERVICE_DIR=..\..\reco_service"
set "MODEL_DIR=..\.."

if not exist "%RECO_SERVICE_DIR%" (
    echo [WARNING] reco_service directory not found: %RECO_SERVICE_DIR%
    echo Model files are in: %MODEL_DIR%
    echo.
    echo Please copy manually:
    echo   - recommendation_model.pkl
    echo   - model_metadata.json
    echo   - user_label_encoder.joblib
    echo   - item_label_encoder.joblib
    echo.
) else (
    copy /Y "%MODEL_DIR%\recommendation_model.pkl" "%RECO_SERVICE_DIR%\recommendation_model.pkl" >nul 2>&1
    copy /Y "%MODEL_DIR%\model_metadata.json" "%RECO_SERVICE_DIR%\model_metadata.json" >nul 2>&1
    copy /Y "%MODEL_DIR%\user_label_encoder.joblib" "%RECO_SERVICE_DIR%\user_label_encoder.joblib" >nul 2>&1
    copy /Y "%MODEL_DIR%\item_label_encoder.joblib" "%RECO_SERVICE_DIR%\item_label_encoder.joblib" >nul 2>&1
    
    echo   ✅ Copied recommendation_model.pkl
    echo   ✅ Copied model_metadata.json
    echo   ✅ Copied user_label_encoder.joblib
    echo   ✅ Copied item_label_encoder.joblib
)

echo.
echo ============================================
echo  ✅ TRAINING COMPLETED!
echo ============================================
echo.
echo Model files location:
echo   - %MODEL_DIR%\recommendation_model.pkl
echo   - %MODEL_DIR%\model_metadata.json
echo   - %MODEL_DIR%\user_label_encoder.joblib
echo   - %MODEL_DIR%\item_label_encoder.joblib
echo.
echo Next steps:
echo   1. Restart the Python ML Service (reco_service/app.py)
echo   2. Test recommendations via API
echo.
pause

