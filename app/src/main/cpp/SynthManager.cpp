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
    : settings(nullptr), synth(nullptr), soundFontId(-1), initialized(false) 
#else
    : initialized(false)
#endif
{
}

SynthManager::~SynthManager() {
    shutdown();
}

int SynthManager::init(const char* soundFontPath) {
#if HAS_FLUIDSYNTH
    settings = new_fluid_settings();
    if (!settings) return -1;

    fluid_settings_setnum(settings, "synth.gain", 0.8);
    fluid_settings_setnum(settings, "synth.sample-rate", 44100.0);

    synth = new_fluid_synth(settings);
    if (!synth) return -1;

    if (soundFontPath && strlen(soundFontPath) > 0) {
        soundFontId = fluid_synth_sfload(synth, soundFontPath, 1);
        if (soundFontId != FLUID_FAILED) {
            fluid_synth_program_select(synth, 0, soundFontId, 0, 0);
        }
    }

    initialized = true;
    return 0;
#else
    return 0;
#endif
}

void SynthManager::render(int numFrames, short* buffer) {
#if HAS_FLUIDSYNTH
    if (synth && initialized) {
        // Render audio to buffer (stereo, 16-bit)
        fluid_synth_write_s16(synth, numFrames, buffer, 0, 2, buffer, 1, 2);
    }
#endif
}

void SynthManager::noteOn(int channel, int note, int velocity) {
#if HAS_FLUIDSYNTH
    if (synth && initialized) fluid_synth_noteon(synth, channel, note, velocity);
#endif
}

void SynthManager::noteOff(int channel, int note) {
#if HAS_FLUIDSYNTH
    if (synth && initialized) fluid_synth_noteoff(synth, channel, note);
#endif
}

void SynthManager::programChange(int channel, int program) {
#if HAS_FLUIDSYNTH
    if (synth && initialized) fluid_synth_program_change(synth, channel, program);
#endif
}

void SynthManager::bankSelect(int channel, int bank) {
#if HAS_FLUIDSYNTH
    if (synth && initialized) fluid_synth_bank_select(synth, channel, bank);
#endif
}

void SynthManager::controlChange(int channel, int controller, int value) {
#if HAS_FLUIDSYNTH
    if (synth && initialized) fluid_synth_cc(synth, channel, controller, value);
#endif
}

void SynthManager::pitchBend(int channel, int value) {
#if HAS_FLUIDSYNTH
    if (synth && initialized) fluid_synth_pitch_bend(synth, channel, value);
#endif
}

void SynthManager::shutdown() {
#if HAS_FLUIDSYNTH
    if (synth) delete_fluid_synth(synth);
    if (settings) delete_fluid_settings(settings);
    synth = nullptr; settings = nullptr;
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

JNIEXPORT void JNICALL Java_com_midibt_controller_synth_SynthEngine_nativeRender(JNIEnv* env, jobject, jshortArray buffer, jint numFrames) {
    if (gSynthManager) {
        jshort* nativeBuffer = env->GetShortArrayElements(buffer, nullptr);
        gSynthManager->render(numFrames, nativeBuffer);
        env->ReleaseShortArrayElements(buffer, nativeBuffer, 0);
    }
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
JNIEXPORT void JNICALL Java_com_midibt_controller_synth_SynthEngine_nativeBankSelect(JNIEnv*, jobject, jint c, jint b) {
    if (gSynthManager) gSynthManager->bankSelect(c, b);
}
JNIEXPORT void JNICALL Java_com_midibt_controller_synth_SynthEngine_nativeControlChange(JNIEnv*, jobject, jint c, jint ct, jint v) {
    if (gSynthManager) gSynthManager->controlChange(c, ct, v);
}
JNIEXPORT void JNICALL Java_com_midibt_controller_synth_SynthEngine_nativePitchBend(JNIEnv*, jobject, jint c, jint v) {
    if (gSynthManager) gSynthManager->pitchBend(c, v);
}
JNIEXPORT void JNICALL Java_com_midibt_controller_synth_SynthEngine_nativeShutdown(JNIEnv*, jobject) {
    if (gSynthManager) { gSynthManager->shutdown(); delete gSynthManager; gSynthManager = nullptr; }
}
}
