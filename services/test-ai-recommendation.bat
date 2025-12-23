@echo off
REM ===================================================
REM TEST AI RECOMMENDATION SERVICE
REM ===================================================
echo.
echo ========================================
echo   TESTING AI RECOMMENDATION SERVICE
echo ========================================
echo.

cd /d %~dp0
powershell -ExecutionPolicy Bypass -File "%~dp0test-ai-recommendation.ps1"

pause


