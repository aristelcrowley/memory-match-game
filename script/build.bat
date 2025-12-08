@echo off
cd /d "%~dp0"
cd ..

echo ==========================================
echo      LOADING CONFIGURATION (.env)
echo ==========================================
if not exist .env (
    echo [ERROR] .env file not found!
    echo Please create one using .env.example as a guide.
    pause
    exit /b
)

:: Read .env file line by line
for /f "usebackq tokens=1* delims==" %%A in (".env") do (
    if "%%A"=="JAVAFX_LIB" set "JAVAFX_LIB=%%B"
    if "%%A"=="JAR_TOOL" set "JAR_TOOL=%%B"
)

echo JavaFX Path: %JAVAFX_LIB%
echo Jar Tool:    %JAR_TOOL%
echo.

echo ==========================================
echo      STEP 1: CLEANING OLD FILES
echo ==========================================
if exist bin rmdir /s /q bin
if exist Client.jar del Client.jar
if exist Server.jar del Server.jar
if exist manifest_client.txt del manifest_client.txt
if exist manifest_server.txt del manifest_server.txt
mkdir bin

echo.
echo ==========================================
echo      STEP 2: COMPILING JAVA CODE
echo ==========================================
javac -d bin --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml,javafx.media src/main/java/com/aristel/server/*.java src/main/java/com/aristel/network/*.java src/main/java/com/aristel/controller/*.java src/main/java/com/aristel/*.java src\main\java\com\aristel\util\*.java

if %errorlevel% neq 0 (
    echo [ERROR] Compilation Failed!
    pause
    exit /b
)

echo.
echo ==========================================
echo      STEP 3: COPYING RESOURCES
echo ==========================================
xcopy "src\main\resources\*" "bin\" /s /e /y /q

echo.
echo ==========================================
echo      STEP 4: CREATING MANIFESTS
echo ==========================================
echo Main-Class: com.aristel.App> manifest_client.txt
echo.>> manifest_client.txt

echo Main-Class: com.aristel.server.GameServer> manifest_server.txt
echo.>> manifest_server.txt

echo.
echo ==========================================
echo      STEP 5: PACKAGING JARS
echo ==========================================
"%JAR_TOOL%" cvfm Client.jar manifest_client.txt -C bin .
"%JAR_TOOL%" cvfm Server.jar manifest_server.txt -C bin .

echo.
echo ==========================================
echo      BUILD COMPLETE!
echo ==========================================
pause