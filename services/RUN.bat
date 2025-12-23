@echo off
REM ===================================================
REM ONE-CLICK STARTUP - Chi can double-click file nay!
REM Includes: Infrastructure + Python ML Service + All Business Services
REM ===================================================
echo.
echo ========================================
echo   STARTING ALL SERVICES
echo ========================================
echo.
echo   - Infrastructure (Config, Discovery, Gateway)
echo   - Python ML Service (AI Recommendations)
echo   - All Business Microservices (17 services)
echo.
echo Please wait...
echo.

cd /d %~dp0
powershell -ExecutionPolicy Bypass -File "%~dp0START.ps1"

pause

