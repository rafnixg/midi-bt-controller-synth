# MIDI BT Controller

Aplicación Android para conectar teclados MIDI vía Bluetooth y reproducir sonidos con selección de bancos estilo VST.

## Requisitos

- Android 8.0 (API 26) o superior
- Dispositivo con Bluetooth LE
- Teclado MIDI compatible con [Bluetooth LE MIDI](https://midi.org/midi-over-bluetooth-le)
- [Android Studio](https://developer.android.com/studio) Meerkat (2024.3.1) o superior
- [NDK](https://developer.android.com/ndk/guides) 25+ (para compilar código nativo)
- [CMake](https://cmake.org/) 3.22.1+

## Características

- 🎹 Conexión a teclados MIDI vía [Bluetooth LE](https://developer.android.com/reference/android/bluetooth/le/package-summary)
- 🎵 Motor de síntesis [FluidSynth](https://www.fluidsynth.org/) con baja latencia vía [Oboe](https://github.com/google/oboe)
- 🎼 Selección de bancos de sonidos [General MIDI](https://midi.org/general-midi-2)
- 📱 Interfaz con [Material Design 3](https://m3.material.io/)

## Inicio Rápido

### 1. Clonar y abrir proyecto

```bash
git clone https://github.com/rafnixg/midi-bt-controller-synth.git
cd midi-bt-controller
# Abrir con Android Studio
```

### 2. Añadir SoundFont (REQUERIDO para audio)

Descarga un SoundFont GM y colócalo en `app/src/main/assets/`:
- [GeneralUser GS](https://schristiancollins.com/generaluser.php) (~30 MB, recomendado)
- [Salamander Piano](https://musical-artifacts.com/artifacts/1009) (~5 MB, solo piano)

### 3. (Opcional) Añadir librerías FluidSynth nativas

Para audio real, necesitas las librerías nativas. Sin ellas, la app funciona en "modo stub" (logs MIDI sin audio).

Ejecuta `scripts\setup_fluidsynth.bat` y sigue las instrucciones, o descarga manualmente:
1. Descarga desde [VolcanoMobile/fluidsynth-android](https://github.com/VolcanoMobile/fluidsynth-android/releases)
2. Extrae en `app/src/main/cpp/fluidsynth/`

### 4. Compilar

```bash
./gradlew assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Estructura del Proyecto

```
app/
├── src/main/
│   ├── java/com/midibt/controller/
│   │   ├── MainActivity.kt              # Actividad principal
│   │   ├── SynthEditorActivity.kt       # Editor de síntesis
│   │   ├── midi/
│   │   │   ├── BluetoothMidiScanner.kt  # Escaneo BLE
│   │   │   └── MidiController.kt        # Manejo MIDI
│   │   ├── synth/
│   │   │   ├── SynthEngine.kt           # Bridge JNI → FluidSynth
│   │   │   └── SoundBank.kt             # Modelo de datos
│   │   └── ui/
│   │       ├── MainViewModel.kt         # ViewModel (Lifecycle)
│   │       ├── DeviceAdapter.kt         # Lista dispositivos
│   │       ├── SoundBankAdapter.kt      # Lista sonidos
│   │       └── OscilloscopeView.kt      # Visualizador de audio
│   ├── cpp/
│   │   ├── CMakeLists.txt
│   │   ├── SynthManager.h
│   │   └── SynthManager.cpp             # Wrapper JNI para FluidSynth
│   └── res/                             # Recursos UI
└── build.gradle.kts
```

## Configuración de FluidSynth

Para habilitar la síntesis de audio, necesitas las librerías precompiladas de [FluidSynth](https://www.fluidsynth.org/):

1. Descarga las librerías de [VolcanoMobile/fluidsynth-android](https://github.com/VolcanoMobile/fluidsynth-android)
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
3. Añade un SoundFont (`.sf2`) en `app/src/main/assets/`

## Dependencias

### Nativas (C++)
| Librería | Versión | Uso |
|---|---|---|
| [FluidSynth](https://www.fluidsynth.org/) | 2.5.0 | Motor de síntesis SF2 |
| [Oboe](https://github.com/google/oboe) | bundled | Audio de baja latencia |
| [libsndfile](https://libsndfile.github.io/libsndfile/) | bundled | Lectura de audio |
| [GLib](https://docs.gtk.org/glib/) | bundled | Utilidades FluidSynth |
| [libFLAC](https://xiph.org/flac/) | bundled | Codec FLAC |
| [Opus](https://opus-codec.org/) | bundled | Codec Opus |
| [libvorbis](https://xiph.org/vorbis/) | bundled | Codec Vorbis/OGG |
| [libinstpatch](http://www.clevercat.org/libinstpatch/) | bundled | Parches de instrumentos |

### Android / Kotlin
| Librería | Versión | Uso |
|---|---|---|
| [Kotlin](https://kotlinlang.org/) | 1.9.21 | Lenguaje principal |
| [Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines) | 1.7.3 | Programación asíncrona |
| [AndroidX Core KTX](https://developer.android.com/kotlin/ktx) | 1.12.0 | Extensiones Kotlin para Android |
| [AndroidX AppCompat](https://developer.android.com/jetpack/androidx/releases/appcompat) | 1.6.1 | Compatibilidad de UI |
| [AndroidX Activity KTX](https://developer.android.com/jetpack/androidx/releases/activity) | 1.8.2 | Extensiones de Activity |
| [AndroidX Lifecycle ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) | 2.7.0 | Arquitectura MVVM |
| [Material Design 3](https://github.com/material-components/material-components-android) | 1.11.0 | Componentes de UI |
| [AndroidX RecyclerView](https://developer.android.com/jetpack/androidx/releases/recyclerview) | 1.3.2 | Listas |
| [ConstraintLayout](https://developer.android.com/jetpack/androidx/releases/constraintlayout) | 2.1.4 | Layout responsivo |

### Herramientas de Build
| Herramienta | Versión |
|---|---|
| [Gradle](https://gradle.org/) | 8.13 |
| [Android Gradle Plugin](https://developer.android.com/build) | 8.13.2 |
| [CMake](https://cmake.org/) | 3.22.1 |
| [NDK](https://developer.android.com/ndk) | 25+ |

## Compilación

```bash
./gradlew assembleDebug   # Debug
./gradlew assembleRelease # Release
```

## Permisos Requeridos

| Permiso | Motivo |
|---|---|
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | Conexión Bluetooth (Android ≤ 11) |
| `ACCESS_FINE_LOCATION` | Requerido para escaneo BLE en Android 8–11 |
| `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` | Conexión Bluetooth (Android 12+) |

## Licencia

[MIT License](LICENSE)
