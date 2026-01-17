package com.midibt.controller.synth

import android.content.Context
import android.util.Log
import com.midibt.controller.BuildConfig
import java.io.File
import java.io.FileOutputStream

class SynthEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SynthEngine"
        private var librariesLoaded = false
        @Volatile
        private var INSTANCE: SynthEngine? = null

        fun getInstance(context: Context): SynthEngine {
            return INSTANCE ?: synchronized(this) {
                val instance = SynthEngine(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

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
        if (!librariesLoaded) return false
        if (isInitialized) return true
        
        try {
            val soundFontPath = copySoundFontFromAssets()
            if (soundFontPath != null) {
                val result = nativeInit(soundFontPath)
                isInitialized = (result == 0)
                return isInitialized
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la inicialización: ${e.message}")
        }
        return false
    }

    fun isReady() = isInitialized

    fun noteOn(channel: Int, note: Int, velocity: Int) {
        if (isInitialized) nativeNoteOn(channel, note, velocity)
    }

    fun noteOff(channel: Int, note: Int) {
        if (isInitialized) nativeNoteOff(channel, note)
    }

    fun programChange(channel: Int, program: Int) {
        if (isInitialized) nativeProgramChange(channel, program)
    }

    fun controlChange(channel: Int, controller: Int, value: Int) {
        if (isInitialized) nativeControlChange(channel, controller, value)
    }

    fun shutdown() {
        if (isInitialized) {
            nativeShutdown()
            isInitialized = false
        }
    }

    private fun copySoundFontFromAssets(): String? {
        try {
            val assetList = context.assets.list("") ?: return null
            val sf = assetList.firstOrNull { it.endsWith(".sf2") } ?: return null
            val file = File(context.filesDir, sf)
            if (!file.exists()) {
                context.assets.open(sf).use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
            }
            return file.absolutePath
        } catch (e: Exception) { return null }
    }

    private external fun nativeInit(path: String): Int
    private external fun nativeNoteOn(c: Int, n: Int, v: Int)
    private external fun nativeNoteOff(c: Int, n: Int)
    private external fun nativeProgramChange(c: Int, p: Int)
    private external fun nativeControlChange(c: Int, ct: Int, v: Int)
    private external fun nativeShutdown()
}
