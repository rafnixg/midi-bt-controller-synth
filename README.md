# MIDI BT Controller

Aplicación Android para conectar teclados MIDI vía Bluetooth y reproducir sonidos con selección de bancos estilo VST.

## Requisitos

- Android 8.0 (API 26) o superior
- Dispositivo con Bluetooth LE
- Teclado MIDI compatible con Bluetooth LE MIDI
- Android Studio Arctic Fox o superior
- NDK 25+ (para compilar código nativo)

## Características

- 🎹 Conexión a teclados MIDI vía Bluetooth LE
- 🎵 Motor de síntesis FluidSynth con baja latencia
- 🎼 Selección de bancos de sonidos General MIDI
- 📱 Interfaz simple con Material Design

## Inicio Rápido

### 1. Clonar y abrir proyecto
```bash
cd c:\projectos\midi-bt-controller
# Abrir con Android Studio
```

### 2. Añadir SoundFont (REQUERIDO para audio)
Descarga un SoundFont GM y colócalo en `app/src/main/assets/`:
- [GeneralUser GS](https://schristiancollins.com/generaluser.php) (~30MB, recomendado)
- [Salamander Piano](https://musical-artifacts.com/artifacts/1009) (~5MB, solo piano)

### 3. (Opcional) Añadir FluidSynth nativo
Para audio real, necesitas las librerías nativas. Sin ellas, la app funciona en "modo stub" (logs MIDI sin audio).

Ejecuta `scripts\setup_fluidsynth.bat` y sigue las instrucciones, o:
1. Descarga de [SourceForge](https://sourceforge.net/projects/fluidsynth.mirror/files/v2.5.0/)
2. Extrae en `app/src/main/cpp/fluidsynth/`

### 4. Compilar
```bash
./gradlew assembleDebug
```

## Estructura del Proyecto

```
app/
├── src/main/
│   ├── java/com/midibt/controller/
│   │   ├── MainActivity.kt           # Actividad principal
│   │   ├── midi/
│   │   │   ├── BluetoothMidiScanner.kt  # Escaneo BLE
│   │   │   └── MidiController.kt        # Manejo MIDI
│   │   ├── synth/
│   │   │   ├── SynthEngine.kt          # Bridge JNI
│   │   │   └── SoundBank.kt            # Modelo de datos
│   │   └── ui/
│   │       ├── MainViewModel.kt        # ViewModel
│   │       ├── DeviceAdapter.kt        # Lista dispositivos
│   │       └── SoundBankAdapter.kt     # Lista sonidos
│   ├── cpp/
│   │   ├── CMakeLists.txt
│   │   ├── SynthManager.h
│   │   └── SynthManager.cpp            # FluidSynth wrapper
│   └── res/                            # Recursos UI
└── build.gradle.kts
```

## Configuración de FluidSynth

Para habilitar la síntesis de audio, necesitas las librerías precompiladas de FluidSynth:

1. Descarga las librerías de [fluidsynth-android](https://github.com/VolcanoMobile/fluidsynth-android)
2. Copia a `app/src/main/cpp/fluidsynth/`:
   ```
   fluidsynth/
   ├── include/
   │   └── fluidsynth.h
   └── lib/
       ├── arm64-v8a/
       │   └── libfluidsynth.so
       ├── armeabi-v7a/
       │   └── libfluidsynth.so
       └── x86_64/
           └── libfluidsynth.so
   ```

3. Añade un SoundFont (.sf2) en `app/src/main/assets/`

## Compilación

```bash
./gradlew assembleDebug
```

## Permisos Requeridos

- `BLUETOOTH` / `BLUETOOTH_ADMIN` - Conexión Bluetooth
- `ACCESS_FINE_LOCATION` - Requerido para escaneo BLE en Android 8-11
- `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` - Android 12+

## Licencia

MIT License
