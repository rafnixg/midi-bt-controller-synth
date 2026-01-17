#include "SynthManager.h"
#include <android/log.h>
#include <string.h>

#define LOG_TAG "SynthManager"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static SynthManager* gSynthManager = nullptr;

SynthManager::SynthManager() 
#if HAS_FLUIDSYNTH
    : settings(nullptr), synth(nullptr), audioDriver(nullptr), soundFontId(-1), initialized(false) 
#else
    : initialized(false)
#endif
{
    LOGI("SynthManager created");
}

SynthManager::~SynthManager() {
    shutdown();
}

int SynthManager::init(const char* soundFontPath) {
#if HAS_FLUIDSYNTH
    LOGI("Initializing FluidSynth. SF Path: %s", soundFontPath);

    settings = new_fluid_settings();
    if (!settings) return -1;

    // --- Configuraciones Generales ---
    fluid_settings_setint(settings, "synth.device-id", 16);
    fluid_settings_setint(settings, "synth.polyphony", 512);
    fluid_settings_setnum(settings, "synth.gain", 0.5);
    fluid_settings_setnum(settings, "synth.sample-rate", 44100.0);
    fluid_settings_setstr(settings, "audio.driver", "oboe");

    // --- Configuraciones de Reverb vía Settings ---
    fluid_settings_setnum(settings, "synth.reverb.room-size", 0.5);
    fluid_settings_setnum(settings, "synth.reverb.damp", 0.3);
    fluid_settings_setnum(settings, "synth.reverb.width", 0.8);
    fluid_settings_setnum(settings, "synth.reverb.level", 0.7);

    // --- Configuraciones de Chorus vía Settings ---
    fluid_settings_setint(settings, "synth.chorus.nr", 4);
    fluid_settings_setnum(settings, "synth.chorus.level", 0.55);
    fluid_settings_setnum(settings, "synth.chorus.speed", 0.36);
    fluid_settings_setnum(settings, "synth.chorus.depth", 3.6);

    synth = new_fluid_synth(settings);
    if (!synth) {
        LOGE("Could not create synthesizer");
        return -1;
    }

    if (soundFontPath && strlen(soundFontPath) > 0) {
        soundFontId = fluid_synth_sfload(synth, soundFontPath, 1);
        if (soundFontId == FLUID_FAILED) {
            LOGE("Failed to load SoundFont: %s", soundFontPath);
        } else {
            LOGI("SoundFont loaded ID: %d", soundFontId);
            fluid_synth_program_select(synth, 0, soundFontId, 0, 0);
        }
    }

    audioDriver = new_fluid_audio_driver(settings, synth);
    if (!audioDriver) {
        LOGW("Oboe failed, trying opensl...");
        fluid_settings_setstr(settings, "audio.driver", "opensl");
        audioDriver = new_fluid_audio_driver(settings, synth);
    }

    if (!audioDriver) {
        LOGE("No audio driver could be initialized!");
        return -1;
    }

    initialized = true;
    LOGI("Synth ready and active with custom Reverb/Chorus settings!");
    return 0;
#else
    LOGE("STUB MODE: HAS_FLUIDSYNTH=0. Audio is DISABLED.");
    return 0;
#endif
}

void SynthManager::noteOn(int channel, int note, int velocity) {
#if HAS_FLUIDSYNTH
    if (synth && initialized) {
        LOGD("MIDI Note On: note=%d, vel=%d", note, velocity);
        fluid_synth_noteon(synth, channel, note, velocity);
    }
#endif
}

void SynthManager::noteOff(int channel, int note) {
#if HAS_FLUIDSYNTH
    if (synth && initialized) {
        fluid_synth_noteoff(synth, channel, note);
    }
#endif
}

void SynthManager::programChange(int channel, int program) {
#if HAS_FLUIDSYNTH
    if (synth && initialized) {
        fluid_synth_program_change(synth, channel, program);
    }
#endif
}

void SynthManager::controlChange(int channel, int controller, int value) {
#if HAS_FLUIDSYNTH
    if (synth && initialized) {
        fluid_synth_cc(synth, channel, controller, value);
    }
#endif
}

void SynthManager::shutdown() {
#if HAS_FLUIDSYNTH
    if (audioDriver) delete_fluid_audio_driver(audioDriver);
    if (synth) delete_fluid_synth(synth);
    if (settings) delete_fluid_settings(settings);
    audioDriver = nullptr; synth = nullptr; settings = nullptr;
#endif
    initialized = false;
}

extern "C" {
JNIEXPORT jint JNICALL Java_com_midibt_controller_synth_SynthEngine_nativeInit(JNIEnv* env, jobject, jstring path) {
    if (gSynthManager) { gSynthManager->shutdown(); delete gSynthManager; }
    gSynthManager = new SynthManager();
    const char* p = env->GetStringUTFChars(path, nullptr);
    int r = gSynthManager->init(p);
    env->ReleaseStringUTFChars(path, p);
    return r;
}
JNIEXPORT void JNICALL Java_com_midibt_controller_synth_SynthEngine_nativeNoteOn(JNIEnv*, jobject, jint c, jint n, jint v) {
    if (gSynthManager) gSynthManager->noteOn(c, n, v);
}
JNIEXPORT void JNICALL Java_com_midibt_controller_synth_SynthEngine_nativeNoteOff(JNIEnv*, jobject, jint c, jint n) {
    if (gSynthManager) gSynthManager->noteOff(c, n);
}
JNIEXPORT void JNICALL Java_com_midibt_controller_synth_SynthEngine_nativeProgramChange(JNIEnv*, jobject, jint c, jint p) {
    if (gSynthManager) gSynthManager->programChange(c, p);
}
JNIEXPORT void JNICALL Java_com_midibt_controller_synth_SynthEngine_nativeControlChange(JNIEnv*, jobject, jint c, jint ct, jint v) {
    if (gSynthManager) gSynthManager->controlChange(c, ct, v);
}
JNIEXPORT void JNICALL Java_com_midibt_controller_synth_SynthEngine_nativeShutdown(JNIEnv*, jobject) {
    if (gSynthManager) { gSynthManager->shutdown(); delete gSynthManager; gSynthManager = nullptr; }
}
}
