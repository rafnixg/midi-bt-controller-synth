package com.midibt.controller.synth

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class SynthEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SynthEngine"
        private const val SAMPLE_RATE = 44100
        private var librariesLoaded = false
        @Volatile private var INSTANCE: SynthEngine? = null

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
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Native library load failed: ${e.message}")
            }
        }
    }

    private var isInitialized = false
    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    private var isPlaying = false
    
    // Callback para pasar los datos al osciloscopio
    private var onBufferRendered: ((ShortArray) -> Unit)? = null

    fun initialize(): Boolean {
        if (!librariesLoaded || isInitialized) return isInitialized
        try {
            val soundFontPath = copySoundFontFromAssets() ?: return false
            if (nativeInit(soundFontPath) == 0) {
                isInitialized = true
                startAudioThread()
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Init error: ${e.message}")
        }
        return false
    }

    fun setOscilloscopeCallback(callback: (ShortArray) -> Unit) {
        this.onBufferRendered = callback
    }

    private fun startAudioThread() {
        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build())
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        isPlaying = true
        audioThread = Thread {
            val numFrames = 256
            val buffer = ShortArray(numFrames * 2) // Estéreo
            audioTrack?.play()
            
            while (isPlaying) {
                nativeRender(buffer, numFrames)
                audioTrack?.write(buffer, 0, buffer.size)
                onBufferRendered?.invoke(buffer)
            }
        }
        audioThread?.start()
    }

    fun noteOn(c: Int, n: Int, v: Int) { if (isInitialized) nativeNoteOn(c, n, v) }
    fun noteOff(c: Int, n: Int) { if (isInitialized) nativeNoteOff(c, n) }
    fun programChange(c: Int, p: Int) { if (isInitialized) nativeProgramChange(c, p) }
    fun bankSelect(c: Int, b: Int) { if (isInitialized) nativeBankSelect(c, b) }
    fun controlChange(c: Int, ct: Int, v: Int) { if (isInitialized) nativeControlChange(c, ct, v) }
    fun pitchBend(c: Int, v: Int) { if (isInitialized) nativePitchBend(c, v) }
    /** Sends All Notes Off (CC 123) on all 16 MIDI channels. */
    fun allNotesOff() { if (isInitialized) for (ch in 0..15) nativeControlChange(ch, 123, 0) }

    fun shutdown() {
        isPlaying = false
        audioThread?.join()
        audioTrack?.stop()
        audioTrack?.release()
        if (isInitialized) nativeShutdown()
        isInitialized = false
    }

    private fun copySoundFontFromAssets(): String? {
        try {
            val sf = context.assets.list("")?.firstOrNull { it.endsWith(".sf2") } ?: return null
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
    private external fun nativeRender(buffer: ShortArray, numFrames: Int)
    private external fun nativeNoteOn(c: Int, n: Int, v: Int)
    private external fun nativeNoteOff(c: Int, n: Int)
    private external fun nativeProgramChange(c: Int, p: Int)
    private external fun nativeBankSelect(c: Int, b: Int)
    private external fun nativeControlChange(c: Int, ct: Int, v: Int)
    private external fun nativePitchBend(c: Int, v: Int)
    private external fun nativeShutdown()
}
