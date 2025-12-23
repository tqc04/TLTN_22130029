@echo off
REM ===================================================
REM GENERATE SAMPLE DATA FOR AI TRAINING
REM ===================================================
echo.
echo ========================================
echo   GENERATE SAMPLE DATA
echo ========================================
echo.

cd /d %~dp0

REM Set database credentials
set DB_HOST=localhost
set DB_PORT=3306
set DB_USER=root
set DB_PASSWORD=123456

echo Setting up database connection...
echo Host: %DB_HOST%
echo Port: %DB_PORT%
echo User: %DB_USER%
echo.

python generate_sample_data.py

echo.
pause


