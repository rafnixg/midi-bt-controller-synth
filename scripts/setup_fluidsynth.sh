#!/bin/bash
# Script para descargar FluidSynth precompilado para Android
# Ejecutar desde la raíz del proyecto

set -e

FLUIDSYNTH_VERSION="2.5.0"
DOWNLOAD_URL="https://sourceforge.net/projects/fluidsynth.mirror/files/v${FLUIDSYNTH_VERSION}/fluidsynth-${FLUIDSYNTH_VERSION}-android24.zip/download"
OUTPUT_DIR="app/src/main/cpp/fluidsynth"

echo "=== FluidSynth Android Setup ==="
echo "Version: $FLUIDSYNTH_VERSION"
echo ""

# Crear directorio de salida
mkdir -p "$OUTPUT_DIR/lib"
mkdir -p "$OUTPUT_DIR/include"

# Descargar FluidSynth
echo "Descargando FluidSynth..."
curl -L -o fluidsynth-android.zip "$DOWNLOAD_URL"

# Extraer
echo "Extrayendo archivos..."
unzip -o fluidsynth-android.zip -d fluidsynth-temp

# Copiar librerías y headers
echo "Copiando librerías..."
if [ -d "fluidsynth-temp/lib" ]; then
    cp -r fluidsynth-temp/lib/* "$OUTPUT_DIR/lib/"
fi

if [ -d "fluidsynth-temp/include" ]; then
    cp -r fluidsynth-temp/include/* "$OUTPUT_DIR/include/"
fi

# Buscar en subdirectorios si la estructura es diferente
find fluidsynth-temp -name "*.so" -exec echo "Found: {}" \;
find fluidsynth-temp -name "fluidsynth.h" -exec echo "Found header: {}" \;

# Limpiar
rm -rf fluidsynth-temp fluidsynth-android.zip

echo ""
echo "=== Estructura creada ==="
ls -la "$OUTPUT_DIR/"
ls -la "$OUTPUT_DIR/lib/" 2>/dev/null || echo "lib/ vacío"
ls -la "$OUTPUT_DIR/include/" 2>/dev/null || echo "include/ vacío"

echo ""
echo "✅ FluidSynth configurado en $OUTPUT_DIR"
echo ""
echo "Siguiente paso: Añadir un SoundFont (.sf2) en app/src/main/assets/"
