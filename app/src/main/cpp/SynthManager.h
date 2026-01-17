#ifndef SYNTH_MANAGER_H
#define SYNTH_MANAGER_H

#include <jni.h>
#include <string>

#if HAS_FLUIDSYNTH
#include <fluidsynth.h>
#endif

class SynthManager {
public:
    SynthManager();
    ~SynthManager();

    int init(const char* soundFontPath);
    int loadSoundFont(const char* path);
    void noteOn(int channel, int note, int velocity);
    void noteOff(int channel, int note);
    void programChange(int channel, int program);
    void controlChange(int channel, int controller, int value);
    void shutdown();

private:
#if HAS_FLUIDSYNTH
    fluid_settings_t* settings = nullptr;
    fluid_synth_t* synth = nullptr;
    fluid_audio_driver_t* audioDriver = nullptr;
    int soundFontId = -1;
#endif
    bool initialized = false;
};

#endif // SYNTH_MANAGER_H
