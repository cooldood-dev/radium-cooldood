@echo off
setlocal EnableExtensions EnableDelayedExpansion

title Radium Client Launcher (Dist)

echo ========================================================
echo          Radium Client Launcher (Standalone Dist)
echo ========================================================
echo.

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

:: Verify binaries exist directly in this dist folder
if not exist "%SCRIPT_DIR%Radium.jar" (
    echo [-] ERROR: Radium.jar not found in "%SCRIPT_DIR%"!
    echo     Please make sure Radium.jar is located in the dist directory.
    pause
    exit /b 1
)

if not exist "%SCRIPT_DIR%Radium.json" (
    echo [-] ERROR: Radium.json not found in "%SCRIPT_DIR%"!
    echo     Please make sure Radium.json is located in the dist directory.
    pause
    exit /b 1
)

:: ---------------------------------------------------------
:: 1. Auto-detect %APPDATA%/.minecraft Directory
:: ---------------------------------------------------------
echo [*] Detecting .minecraft directory...

set "MINECRAFT_DIR="

if defined APPDATA (
    if exist "%APPDATA%\.minecraft" (
        set "MINECRAFT_DIR=%APPDATA%\.minecraft"
    )
)

if not defined MINECRAFT_DIR (
    if defined USERPROFILE (
        if exist "%USERPROFILE%\AppData\Roaming\.minecraft" (
            set "MINECRAFT_DIR=%USERPROFILE%\AppData\Roaming\.minecraft"
        )
    )
)

if not defined MINECRAFT_DIR (
    echo [!] Warning: .minecraft not found in %%APPDATA%%. Creating directory...
    mkdir "%APPDATA%\.minecraft" 2>nul
    set "MINECRAFT_DIR=%APPDATA%\.minecraft"
)

echo [+] Minecraft Directory: "%MINECRAFT_DIR%"
echo.

:: ---------------------------------------------------------
:: 2. Auto-detect Required Java Version (Java 8 / 1.8)
:: ---------------------------------------------------------
echo [*] Detecting required Java 8 (1.8) runtime...

set "JAVA_TEMP=%TEMP%\radium_java_%RANDOM%.txt"
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%detect.ps1" -FindJava > "%JAVA_TEMP%"

set "TARGET_JAVA="
if exist "%JAVA_TEMP%" (
    set /p TARGET_JAVA=<"%JAVA_TEMP%"
    del "%JAVA_TEMP%" 2>nul
)

if not defined TARGET_JAVA (
    echo.
    echo [-] ERROR: Required Java 8 was not found on your system!
    echo     Please install Java 8 JRE or JDK to launch Radium Client.
    echo.
    pause
    exit /b 1
)

echo [+] Using Java: "%TARGET_JAVA%"
"%TARGET_JAVA%" -version
echo.

:: ---------------------------------------------------------
:: 3. Launch Radium Client using binaries strictly in dist
:: ---------------------------------------------------------
echo [*] Launching Radium Client using binaries in "%SCRIPT_DIR%"...
echo.

if "%~1"=="" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%detect.ps1" -Launch -ScriptDir "%SCRIPT_DIR:~0,-1%" -MinecraftDir "%MINECRAFT_DIR%" -JavaExe "%TARGET_JAVA%"
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%detect.ps1" -Launch -ScriptDir "%SCRIPT_DIR:~0,-1%" -MinecraftDir "%MINECRAFT_DIR%" -JavaExe "%TARGET_JAVA%" -GameArgs %*
)

set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
    echo.
    echo [!] Client exited with code %EXIT_CODE%.
)

cd /d "%SCRIPT_DIR%"
exit /b %EXIT_CODE%
