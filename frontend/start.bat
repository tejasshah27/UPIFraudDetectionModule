@echo off
setlocal

set PORT=4202

echo ============================================
echo  UPI Fraud Detection - Frontend (Angular)
echo  Port: %PORT%
echo ============================================
echo.

:: Check if something is already running on port 4202
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%PORT% " ^| findstr "LISTENING"') do (
    set PID=%%a
)

if defined PID (
    echo [!] Process found on port %PORT% (PID: %PID%) - killing it...
    taskkill /PID %PID% /F >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] Failed to kill process %PID%. Try running as Administrator.
        pause
        exit /b 1
    )
    echo [OK] Process %PID% killed.
    timeout /t 2 /nobreak >nul
) else (
    echo [OK] Port %PORT% is free.
)

echo.
echo [>>] Starting Angular dev server...
echo.
call npm start

endlocal
