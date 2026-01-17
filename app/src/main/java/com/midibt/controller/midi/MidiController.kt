package com.midibt.controller.midi

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import android.util.Log

class MidiController private constructor(context: Context) {

    private val midiManager: MidiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    private var midiDevice: MidiDevice? = null
    private var midiOutputPort: MidiOutputPort? = null
    private var midiCallback: ((status: Int, data1: Int, data2: Int) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "MidiController"
        @Volatile private var INSTANCE: MidiController? = null

        fun getInstance(context: Context): MidiController {
            return INSTANCE ?: synchronized(this) {
                val instance = MidiController(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice, onResult: (MidiDevice?, Boolean) -> Unit) {
        Log.d(TAG, "Conectando a: ${device.name}")
        midiManager.openBluetoothDevice(device, { deviceResult ->
            if (deviceResult != null) {
                this.midiDevice = deviceResult
                setupMidiInput(deviceResult)
                onResult(deviceResult, true)
            } else {
                onResult(null, false)
            }
        }, handler)
    }

    private fun setupMidiInput(device: MidiDevice) {
        try {
            midiOutputPort?.close()
            val info = device.info
            if (info.outputPortCount > 0) {
                midiOutputPort = device.openOutputPort(0)
                midiOutputPort?.connect(midiReceiver)
                Log.i(TAG, "MidiReceiver conectado exitosamente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en setupMidiInput: ${e.message}")
        }
    }

    fun setMidiCallback(callback: (Int, Int, Int) -> Unit) {
        Log.d(TAG, "Nuevo callback MIDI configurado")
        this.midiCallback = callback
    }

    private val midiReceiver = object : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            var i = offset
            while (i < offset + count) {
                val status = data[i].toInt() and 0xFF
                if (status >= 0xF8) { i++; continue } // Ignorar real-time
                
                if (i + 2 < offset + count) {
                    val d1 = data[i + 1].toInt() and 0x7F
                    val d2 = data[i + 2].toInt() and 0x7F
                    
                    // Log siempre visible para verificar que entran datos
                    Log.v(TAG, "MIDI IN: 0x${"%02X".format(status)} $d1 $d2")
                    
                    midiCallback?.invoke(status, d1, d2)
                    
                    // Determinar longitud del mensaje
                    i += when (status and 0xF0) {
                        0xC0, 0xD0 -> 2
                        else -> 3
                    }
                } else i++
            }
        }
    }

    fun disconnect() {
        Log.w(TAG, "Desconectando manualmente...")
        midiOutputPort?.close()
        midiDevice?.close()
        midiOutputPort = null
        midiDevice = null
    }
}
