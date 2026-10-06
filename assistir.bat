@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0comparar.ps1" -Segundos 30 -Assistir
if errorlevel 1 pause
