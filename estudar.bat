@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0estudar.ps1"
if errorlevel 1 pause
