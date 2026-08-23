@echo off
chcp 65001 >nul
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\configurator.ps1" restore
set "CONFIGURATOR_EXIT=%ERRORLEVEL%"
echo.
pause
exit /b %CONFIGURATOR_EXIT%
