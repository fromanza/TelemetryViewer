@echo off
setlocal enabledelayedexpansion
REM Creates a "fat JAR" that includes all dependencies
REM This JAR can be double-clicked without needing the lib folder
REM Distribution-ready: Just share this single JAR file

set SRC_DIR=src
set LIB_DIR=lib
set OUT_DIR=out
set CLASSES_DIR=%OUT_DIR%\classes
set FAT_JAR_NAME=TelemetryViewer.jar
set TEMP_DIR=%OUT_DIR%\temp-jar
set LOG_FILE=build.log

REM Create log file with timestamp header
echo ======================================== >> "%LOG_FILE%"
echo Build Started: %date% %time% >> "%LOG_FILE%"
echo ======================================== >> "%LOG_FILE%"
echo. >> "%LOG_FILE%"

echo ========================================
echo Building Standalone Telemetry Viewer JAR
echo (includes all dependencies)
echo ========================================
echo.
echo Logging to: %LOG_FILE%
echo.

REM Clean previous build
if exist "%CLASSES_DIR%" rmdir /s /q "%CLASSES_DIR%" >> "%LOG_FILE%" 2>&1
if exist "%TEMP_DIR%" rmdir /s /q "%TEMP_DIR%" >> "%LOG_FILE%" 2>&1
if exist "%OUT_DIR%\%FAT_JAR_NAME%" del "%OUT_DIR%\%FAT_JAR_NAME%" >> "%LOG_FILE%" 2>&1

REM Create output directories
mkdir "%CLASSES_DIR%" 2>> "%LOG_FILE%"
mkdir "%OUT_DIR%" 2>> "%LOG_FILE%"
mkdir "%TEMP_DIR%" 2>> "%LOG_FILE%"

REM Build classpath from all JARs in lib directory
set CLASSPATH=
for %%f in ("%LIB_DIR%\*.jar") do (
    if "!CLASSPATH!"=="" (
        set CLASSPATH=%%f
    ) else (
        set CLASSPATH=!CLASSPATH!;%%f
    )
)

REM Compile all Java files
echo [1/5] Compiling Java source files...
echo [1/5] Compiling Java source files... >> "%LOG_FILE%"
set JAVA_FILES=
for %%f in ("%SRC_DIR%\*.java") do (
    if "!JAVA_FILES!"=="" (
        set JAVA_FILES=%%f
    ) else (
        set JAVA_FILES=!JAVA_FILES! %%f
    )
)
javac -d "%CLASSES_DIR%" -cp "%CLASSPATH%" !JAVA_FILES! >> "%LOG_FILE%" 2>&1
if errorlevel 1 (
    echo.
    echo ERROR: Compilation failed! Check %LOG_FILE% for details.
    echo ERROR: Compilation failed! >> "%LOG_FILE%"
    pause
    exit /b 1
)

REM Copy resources
if exist "resources" (
    echo [2/5] Copying resources...
    echo [2/5] Copying resources... >> "%LOG_FILE%"
    xcopy /E /I /Y "resources" "%CLASSES_DIR%\resources\" >> "%LOG_FILE%" 2>&1
) else (
    echo [2/5] No resources to copy...
    echo [2/5] No resources to copy... >> "%LOG_FILE%"
)

REM Copy compiled classes to temp directory
echo [3/5] Preparing compiled classes...
echo [3/5] Preparing compiled classes... >> "%LOG_FILE%"
xcopy /E /I /Y "%CLASSES_DIR%\*" "%TEMP_DIR%\" >> "%LOG_FILE%" 2>&1

REM Extract all JAR files from lib into temp directory
echo [4/5] Extracting dependencies from lib folder...
echo [4/5] Extracting dependencies from lib folder... >> "%LOG_FILE%"
set JAR_COUNT=0
for %%f in ("%LIB_DIR%\*.jar") do (
    set /a JAR_COUNT+=1
    echo   [!JAR_COUNT!] Extracting %%~nxf...
    echo   [!JAR_COUNT!] Extracting %%~nxf... >> "%LOG_FILE%"
    cd "%TEMP_DIR%"
    jar xf "..\..\%LIB_DIR%\%%~nxf" >> "..\..\%LOG_FILE%" 2>&1
    cd "..\.."
)

REM Remove META-INF from extracted JARs (to avoid manifest conflicts)
REM Keep only the one we create
if exist "%TEMP_DIR%\META-INF" (
    rmdir /s /q "%TEMP_DIR%\META-INF" >> "%LOG_FILE%" 2>&1
)

REM Create manifest with Main-Class
echo [5/5] Creating JAR file...
echo [5/5] Creating JAR file... >> "%LOG_FILE%"
echo Main-Class: Main > "%OUT_DIR%\manifest.txt"
echo Class-Path: . >> "%OUT_DIR%\manifest.txt"

REM Create fat JAR file
cd "%TEMP_DIR%"
jar cfm "..\%FAT_JAR_NAME%" "..\manifest.txt" * >> "..\..\%LOG_FILE%" 2>&1
cd "..\.."

if errorlevel 1 (
    echo.
    echo ERROR: JAR creation failed! Check %LOG_FILE% for details.
    echo ERROR: JAR creation failed! >> "%LOG_FILE%"
    pause
    exit /b 1
)

REM Get file size
for %%A in ("%OUT_DIR%\%FAT_JAR_NAME%") do set SIZE=%%~zA
set /a SIZE_MB=%SIZE% / 1048576

REM Cleanup temp directory
rmdir /s /q "%TEMP_DIR%" >> "%LOG_FILE%" 2>&1

echo.
echo ========================================
echo Build Successful!
echo ========================================
echo.
echo Output: %OUT_DIR%\%FAT_JAR_NAME%
echo Size: ~%SIZE_MB% MB
echo.
echo This JAR is standalone and can be:
echo   - Double-clicked to run
echo   - Distributed to others (no lib folder needed)
echo   - Run with: java -jar %OUT_DIR%\%FAT_JAR_NAME%
echo.

REM Log success message
echo ======================================== >> "%LOG_FILE%"
echo Build Successful! >> "%LOG_FILE%"
echo Output: %OUT_DIR%\%FAT_JAR_NAME% >> "%LOG_FILE%"
echo Size: ~%SIZE_MB% MB >> "%LOG_FILE%"
echo Build Completed: %date% %time% >> "%LOG_FILE%"
echo. >> "%LOG_FILE%"

pause

