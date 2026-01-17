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
    void render(int numFrames, short* buffer); // <--- NUEVO: Para obtener los samples
    void noteOn(int channel, int note, int velocity);
    void noteOff(int channel, int note);
    void programChange(int channel, int program);
    void bankSelect(int channel, int bank);
    void controlChange(int channel, int controller, int value);
    void shutdown();

private:
#if HAS_FLUIDSYNTH
    fluid_settings_t* settings = nullptr;
    fluid_synth_t* synth = nullptr;
    // Quitamos audioDriver para manejarlo nosotros
    int soundFontId = -1;
#endif
    bool initialized = false;
};

#endif
