# FluidSynth Libraries for Android

This directory should contain the precompiled FluidSynth libraries for Android.

## Download Instructions

### Option 1: SourceForge (Official)

1. Download from: https://sourceforge.net/projects/fluidsynth.mirror/files/v2.5.0/
2. Get `fluidsynth-2.5.0-android24.zip`
3. Extract and copy files here:

```
fluidsynth/
├── include/
│   ├── fluidsynth.h
│   └── fluidsynth/
│       ├── audio.h
│       ├── event.h
│       ├── gen.h
│       ├── ladspa.h
│       ├── log.h
│       ├── midi.h
│       ├── misc.h
│       ├── mod.h
│       ├── seq.h
│       ├── seqbind.h
│       ├── settings.h
│       ├── sfont.h
│       ├── shell.h
│       ├── synth.h
│       ├── types.h
│       └── version.h
└── lib/
    ├── arm64-v8a/
    │   └── libfluidsynth.so
    ├── armeabi-v7a/
    │   └── libfluidsynth.so
    └── x86_64/
        └── libfluidsynth.so
```

### Option 2: Build from Source

See: https://github.com/FluidSynth/fluidsynth/wiki/BuildingForAndroid

## Without FluidSynth

The app will compile and run without FluidSynth in "stub mode":
- MIDI messages will be logged but no audio will be produced
- Useful for testing Bluetooth connectivity
