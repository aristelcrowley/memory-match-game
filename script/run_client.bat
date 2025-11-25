@echo off
cd /d "%~dp0"
cd ..
title Memory Game CLIENT

:: Load Config
if not exist .env (
    echo [ERROR] .env file not found!
    pause
    exit /b
)
for /f "usebackq tokens=1* delims==" %%A in (".env") do (
    if "%%A"=="JAVAFX_LIB" set "JAVAFX_LIB=%%B"
)

echo Starting Client...
java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml -jar Client.jar
pause