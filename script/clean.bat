@echo off
cd /d "%~dp0"
cd ..

echo ==========================================
echo      CLEANING PROJECT...
echo ==========================================

:: 1. Delete the compiled bin folder
if exist bin (
    echo Deleting bin folder...
    rmdir /s /q bin
)

:: 2. Delete the JAR files
if exist Client.jar (
    echo Deleting Client.jar...
    del Client.jar
)
if exist Server.jar (
    echo Deleting Server.jar...
    del Server.jar
)

:: 3. Delete the temporary Manifest files
if exist manifest_client.txt (
    echo Deleting manifest_client.txt...
    del manifest_client.txt
)
if exist manifest_server.txt (
    echo Deleting manifest_server.txt...
    del manifest_server.txt
)

echo.
echo ==========================================
echo      CLEAN COMPLETE!
echo ==========================================
pause