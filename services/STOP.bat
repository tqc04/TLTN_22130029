@echo off
REM ===================================================
REM STOP ALL SERVICES - Double-click de stop tat ca
REM ===================================================
echo.
echo ========================================
echo   STOPPING ALL JAVA PROCESSES...
echo ========================================
echo.

taskkill /F /IM java.exe 2>nul
if %errorlevel% == 0 (
    echo All services stopped!
) else (
    echo No Java processes found.
)

echo.
pause

