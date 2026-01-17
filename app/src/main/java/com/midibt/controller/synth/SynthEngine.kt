package com.midibt.controller.synth

import android.content.Context
import android.util.Log
import com.midibt.controller.BuildConfig
import java.io.File
import java.io.FileOutputStream

class SynthEngine(private val context: Context) {

    companion object {
        private const val TAG = "SynthEngine"
        private var librariesLoaded = false
        
        init {
            try {
                System.loadLibrary("synth-lib")
                librariesLoaded = true
                Log.d(TAG, "Librerías nativas cargadas con éxito")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "ERROR: No se pudieron cargar las librerías: ${e.message}")
            }
        }
    }

    private var isInitialized = false

    fun initialize(): Boolean {
        if (!librariesLoaded) {
            Log.e(TAG, "No se puede inicializar: Librerías no cargadas")
            return false
        }
        if (isInitialized) return true
        
        try {
            val soundFontPath = copySoundFontFromAssets()
            if (soundFontPath != null) {
                Log.d(TAG, "Inicializando motor con SoundFont: $soundFontPath")
                val result = nativeInit(soundFontPath)
                isInitialized = (result == 0)
                Log.d(TAG, "Resultado inicialización nativa: ${if (isInitialized) "ÉXITO" else "FALLO ($result)"}")
                return isInitialized
            } else {
                Log.e(TAG, "ERROR: No se encontró ningún archivo .sf2 en assets")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la inicialización: ${e.message}")
        }
        return false
    }

    fun noteOn(channel: Int, note: Int, velocity: Int) {
        if (isInitialized) {
            if (BuildConfig.DEBUG) Log.v(TAG, "DEBUG MIDI -> Note On: nota=$note, vel=$velocity")
            nativeNoteOn(channel, note, velocity)
        } else {
            Log.w(TAG, "Intento de NoteOn sin motor inicializado")
        }
    }

    fun noteOff(channel: Int, note: Int) {
        if (isInitialized) {
            if (BuildConfig.DEBUG) Log.v(TAG, "DEBUG MIDI -> Note Off: nota=$note")
            nativeNoteOff(channel, note)
        }
    }

    fun programChange(channel: Int, program: Int) {
        if (isInitialized) {
            Log.i(TAG, "Cambio de instrumento: $program")
            nativeProgramChange(channel, program)
        }
    }

    fun controlChange(channel: Int, controller: Int, value: Int) {
        if (isInitialized) {
            nativeControlChange(channel, controller, value)
        }
    }

    fun shutdown() {
        if (isInitialized) {
            nativeShutdown()
            isInitialized = false
            Log.d(TAG, "Synth shutdown")
        }
    }

    private fun copySoundFontFromAssets(): String? {
        try {
            val assetList = context.assets.list("") ?: return null
            val sf = assetList.firstOrNull { it.endsWith(".sf2") } ?: return null
            val file = File(context.filesDir, sf)
            
            if (!file.exists()) {
                Log.d(TAG, "Copiando SoundFont $sf a memoria interna...")
                context.assets.open(sf).use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                Log.d(TAG, "Copia finalizada")
            }
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error al copiar SoundFont: ${e.message}")
            return null
        }
    }

    private external fun nativeInit(path: String): Int
    private external fun nativeNoteOn(c: Int, n: Int, v: Int)
    private external fun nativeNoteOff(c: Int, n: Int)
    private external fun nativeProgramChange(c: Int, p: Int)
    private external fun nativeControlChange(c: Int, ct: Int, v: Int)
    private external fun nativeShutdown()
}
