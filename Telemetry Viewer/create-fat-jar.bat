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

echo ========================================
echo Building Standalone Telemetry Viewer JAR
echo (includes all dependencies)
echo ========================================
echo.

REM Clean previous build
if exist "%CLASSES_DIR%" rmdir /s /q "%CLASSES_DIR%"
if exist "%TEMP_DIR%" rmdir /s /q "%TEMP_DIR%"
if exist "%OUT_DIR%\%FAT_JAR_NAME%" del "%OUT_DIR%\%FAT_JAR_NAME%"

REM Create output directories
mkdir "%CLASSES_DIR%" 2>nul
mkdir "%OUT_DIR%" 2>nul
mkdir "%TEMP_DIR%"

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
set JAVA_FILES=
for %%f in ("%SRC_DIR%\*.java") do (
    if "!JAVA_FILES!"=="" (
        set JAVA_FILES=%%f
    ) else (
        set JAVA_FILES=!JAVA_FILES! %%f
    )
)
javac -d "%CLASSES_DIR%" -cp "%CLASSPATH%" !JAVA_FILES!
if errorlevel 1 (
    echo.
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

REM Copy resources
if exist "resources" (
    echo [2/5] Copying resources...
    xcopy /E /I /Y "resources" "%CLASSES_DIR%\resources\" >nul
) else (
    echo [2/5] No resources to copy...
)

REM Copy compiled classes to temp directory
echo [3/5] Preparing compiled classes...
xcopy /E /I /Y "%CLASSES_DIR%\*" "%TEMP_DIR%\" >nul

REM Extract all JAR files from lib into temp directory
echo [4/5] Extracting dependencies from lib folder...
set JAR_COUNT=0
for %%f in ("%LIB_DIR%\*.jar") do (
    set /a JAR_COUNT+=1
    echo   [!JAR_COUNT!] Extracting %%~nxf...
    cd "%TEMP_DIR%"
    jar xf "..\..\%LIB_DIR%\%%~nxf" >nul 2>&1
    cd "..\.."
)

REM Remove META-INF from extracted JARs (to avoid manifest conflicts)
REM Keep only the one we create
if exist "%TEMP_DIR%\META-INF" (
    rmdir /s /q "%TEMP_DIR%\META-INF"
)

REM Create manifest with Main-Class
echo [5/5] Creating JAR file...
echo Main-Class: Main > "%OUT_DIR%\manifest.txt"
echo Class-Path: . >> "%OUT_DIR%\manifest.txt"

REM Create fat JAR file
cd "%TEMP_DIR%"
jar cfm "..\%FAT_JAR_NAME%" "..\manifest.txt" * >nul
cd "..\.."

if errorlevel 1 (
    echo.
    echo ERROR: JAR creation failed!
    pause
    exit /b 1
)

REM Get file size
for %%A in ("%OUT_DIR%\%FAT_JAR_NAME%") do set SIZE=%%~zA
set /a SIZE_MB=%SIZE% / 1048576

REM Cleanup temp directory
rmdir /s /q "%TEMP_DIR%"

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
pause

