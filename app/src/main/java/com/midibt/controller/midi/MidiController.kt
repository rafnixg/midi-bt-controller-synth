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

class MidiController private constructor(private val midiManager: MidiManager) {

    companion object {
        private const val TAG = "MidiController"
        @Volatile
        private var INSTANCE: MidiController? = null

        fun getInstance(context: Context): MidiController {
            return INSTANCE ?: synchronized(this) {
                val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
                val instance = MidiController(midiManager)
                INSTANCE = instance
                instance
            }
        }
    }

    private var midiDevice: MidiDevice? = null
    private var midiOutputPort: MidiOutputPort? = null
    private var midiCallback: ((status: Int, data1: Int, data2: Int) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice, onResult: (MidiDevice?, Boolean) -> Unit) {
        Log.d(TAG, "Connecting to MIDI device: ${device.name}")
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
        if (device.info.outputPortCount > 0) {
            midiOutputPort = device.openOutputPort(0)
            midiOutputPort?.connect(midiReceiver)
            Log.d(TAG, "MidiReceiver connected and listening")
        }
    }

    fun setMidiCallback(callback: (status: Int, data1: Int, data2: Int) -> Unit) {
        this.midiCallback = callback
    }

    private val midiReceiver = object : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            var i = offset
            while (i < offset + count) {
                val status = data[i].toInt() and 0xFF
                if (status >= 0xF8) { i++; continue }
                
                when (status and 0xF0) {
                    0x90, 0x80, 0xB0, 0xE0 -> {
                        if (i + 2 < offset + count) {
                            val d1 = data[i + 1].toInt() and 0x7F
                            val d2 = data[i + 2].toInt() and 0x7F
                            midiCallback?.invoke(status, d1, d2)
                            i += 3
                        } else i++
                    }
                    0xC0, 0xD0 -> {
                        if (i + 1 < offset + count) {
                            midiCallback?.invoke(status, data[i + 1].toInt() and 0x7F, 0)
                            i += 2
                        } else i++
                    }
                    else -> i++
                }
            }
        }
    }

    fun disconnect() {
        midiOutputPort?.close()
        midiDevice?.close()
        midiOutputPort = null
        midiDevice = null
    }
}
