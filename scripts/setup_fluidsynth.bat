@echo off
REM Script para configurar FluidSynth en Windows
REM Ejecutar desde la raíz del proyecto con PowerShell o CMD

echo === FluidSynth Android Setup (Windows) ===
echo.

set FLUIDSYNTH_VERSION=2.5.0
set DOWNLOAD_URL=https://sourceforge.net/projects/fluidsynth.mirror/files/v%FLUIDSYNTH_VERSION%/fluidsynth-%FLUIDSYNTH_VERSION%-android24.zip/download
set OUTPUT_DIR=app\src\main\cpp\fluidsynth

REM Crear directorios
mkdir "%OUTPUT_DIR%\lib\arm64-v8a" 2>nul
mkdir "%OUTPUT_DIR%\lib\armeabi-v7a" 2>nul
mkdir "%OUTPUT_DIR%\lib\x86_64" 2>nul
mkdir "%OUTPUT_DIR%\include" 2>nul

echo Descargando FluidSynth %FLUIDSYNTH_VERSION%...
echo URL: %DOWNLOAD_URL%
echo.
echo Por favor descarga manualmente desde:
echo   https://sourceforge.net/projects/fluidsynth.mirror/files/v%FLUIDSYNTH_VERSION%/
echo.
echo Y extrae los archivos en: %OUTPUT_DIR%
echo.
echo Estructura esperada:
echo   %OUTPUT_DIR%\
echo   ├── include\
echo   │   └── fluidsynth.h (y otros headers)
echo   └── lib\
echo       ├── arm64-v8a\
echo       │   └── libfluidsynth.so
echo       ├── armeabi-v7a\
echo       │   └── libfluidsynth.so
echo       └── x86_64\
echo           └── libfluidsynth.so
echo.

REM Crear assets directory
mkdir "app\src\main\assets" 2>nul

echo Siguiente paso: Añadir SoundFont (.sf2) en app\src\main\assets\
echo.
echo SoundFonts recomendados (gratis):
echo   - GeneralUser GS (~30MB): https://schristiancollins.com/generaluser.php
echo   - FluidR3_GM (~148MB): https://member.keymusician.com/Member/FluidR3_GM/index.html
echo   - Salamander C5 Light: https://musical-artifacts.com/artifacts/1009
echo.
pause
