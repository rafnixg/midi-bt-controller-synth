# MIDI Bluetooth Controller - Lista de Tareas

## 🎯 Objetivo del Proyecto
Crear una aplicación Android 8.0 (API 26) que conecte vía Bluetooth a teclados MIDI y permita seleccionar bancos de sonidos estilo VST usando FluidSynth como motor de síntesis.

## 📋 Tareas

### Fase 1: Setup del Proyecto ✅
- [x] **1. Crear estructura proyecto Android**
  - Configurar Gradle con Kotlin DSL
  - Configurar NDK y CMake para código nativo
  - Añadir dependencias (Material Design, Coroutines, etc.)

- [x] **2. Configurar permisos y manifest**
  - Permisos Bluetooth (BLUETOOTH, BLUETOOTH_ADMIN)
  - Permisos ubicación (ACCESS_FINE_LOCATION)
  - Feature MIDI y BLE

### Fase 2: Bluetooth MIDI ✅
- [x] **3. Implementar escaneo Bluetooth**
  - BluetoothLeScanner con filtro UUID MIDI BLE
  - UI para mostrar dispositivos encontrados
  - Manejo de permisos en runtime

- [x] **4. Conexión MIDI Manager**
  - Abrir dispositivo con MidiManager.openBluetoothDevice()
  - Implementar MidiReceiver para recibir datos
  - Parsear mensajes Note On/Off, Control Change

### Fase 3: Motor de Síntesis ✅
- [x] **5. Integrar FluidSynth vía JNI**
  - Configurar CMakeLists.txt con FluidSynth precompilado
  - Crear SynthManager.cpp con wrapper JNI
  - Configurar Oboe como driver de audio (baja latencia)

- [x] **6. Gestión de SoundFonts**
  - Código para cargar .sf2 con fluid_synth_sfload()
  - Cambiar presets con fluid_synth_program_change()
  - Lista de 38 presets General MIDI

### Fase 4: Interfaz de Usuario ✅
- [x] **7. Crear UI principal**
  - Card con estado de conexión Bluetooth
  - Botón de escaneo de dispositivos
  - RecyclerView con lista de bancos de sonidos
  - Selector de programa/preset

### Fase 5: Pendiente
- [x] **8. Añadir librerías nativas y assets**
  - Scripts de configuración creados (setup_fluidsynth.bat/sh)
  - Directorios de FluidSynth preparados
  - README con instrucciones de descarga de SoundFonts
  - Gradle wrapper configurado

## 📥 Pasos para Completar

### Descargar SoundFont (REQUERIDO)
1. Ir a https://schristiancollins.com/generaluser.php
2. Descargar GeneralUser GS (~30MB)
3. Copiar el .sf2 a `app/src/main/assets/`

### Descargar FluidSynth (OPCIONAL - para audio real)
1. Ejecutar `scripts\setup_fluidsynth.bat`
2. Descargar de SourceForge: https://sourceforge.net/projects/fluidsynth.mirror/files/v2.5.0/
3. Extraer en `app/src/main/cpp/fluidsynth/`

### Compilar
```bash
# Abrir en Android Studio o ejecutar:
./gradlew assembleDebug
```

## 🔧 Stack Técnico
- **Lenguaje:** Kotlin + C++ (JNI)
- **Min SDK:** 26 (Android 8.0)
- **Audio:** FluidSynth + Oboe
- **UI:** Material Design Components
- **Build:** Gradle Kotlin DSL + CMake

## 📦 Dependencias Principales
- androidx.core:core-ktx
- com.google.android.material:material
- androidx.lifecycle:lifecycle-viewmodel-ktx
- org.jetbrains.kotlinx:kotlinx-coroutines-android
- FluidSynth 2.3.x (precompiled)
- Google Oboe (audio driver)

## 🎹 Recursos
- **SoundFont recomendado:** GeneralUser GS (~30MB)
- **UUID MIDI BLE:** 03B80E5A-EDE8-4B33-A751-6CE34EC4C700
