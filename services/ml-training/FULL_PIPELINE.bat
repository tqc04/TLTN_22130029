@echo off
REM ===================================================
REM FULL AI TRAINING PIPELINE
REM Generate Data -> Extract -> Train -> Deploy
REM ===================================================
echo.
echo ========================================
echo   FULL AI TRAINING PIPELINE
echo ========================================
echo.

cd /d %~dp0

REM Set database credentials
set DB_HOST=localhost
set DB_PORT=3306
set DB_USER=root
set DB_PASSWORD=123456

echo [1/4] Generating sample data...
echo.
python generate_sample_data.py
if errorlevel 1 (
    echo.
    echo ERROR: Failed to generate data
    pause
    exit /b 1
)

echo.
echo ========================================
echo.
echo [2/4] Extracting training data...
echo.
python extract_training_data.py
if errorlevel 1 (
    echo.
    echo ERROR: Failed to extract data
    pause
    exit /b 1
)

echo.
echo ========================================
echo.
echo [3/4] Training model...
echo.
python train_simple_model.py
if errorlevel 1 (
    echo.
    echo ERROR: Failed to train model
    pause
    exit /b 1
)

echo.
echo ========================================
echo.
echo [4/4] Copying model files to reco_service...
echo.

set SOURCE_DIR=%~dp0..\..\
set TARGET_DIR=%~dp0..\..\reco_service\

copy "%SOURCE_DIR%recommendation_model.pkl" "%TARGET_DIR%" /Y
copy "%SOURCE_DIR%user_label_encoder.joblib" "%TARGET_DIR%" /Y
copy "%SOURCE_DIR%item_label_encoder.joblib" "%TARGET_DIR%" /Y
copy "%SOURCE_DIR%model_metadata.json" "%TARGET_DIR%" /Y

echo.
echo ========================================
echo   PIPELINE COMPLETE!
echo ========================================
echo.
echo Model files copied to reco_service/
echo.
echo Next steps:
echo   1. Restart Python ML Service
echo   2. Test: cd ..\services
echo   3. Run: .\test-ai-recommendation.bat
echo.

pause


