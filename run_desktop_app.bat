@echo off
title Astro Enterprise Studio
cd /d "%~dp0"

set "PYTHON_EXE=%~dp0target\engine-venv\Scripts\python.exe"
set "PYTHONW_EXE=%~dp0target\engine-venv\Scripts\pythonw.exe"

if not exist "%PYTHON_EXE%" (
    echo [ERROR] Virtual environment python.exe not found at %PYTHON_EXE%
    pause
    exit /b 1
)

echo =======================================================
echo   Launching Astro Enterprise Studio Desktop Suite...
echo =======================================================

start "" "%PYTHONW_EXE%" "%~dp0desktop\run_app.py"
exit
