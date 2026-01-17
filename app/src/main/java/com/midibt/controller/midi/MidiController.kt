package com.midibt.controller.midi

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.midibt.controller.BuildConfig

/**
 * Manages MIDI device connections and message handling
 */
class MidiController(private val midiManager: MidiManager) {

    companion object {
        private const val TAG = "MidiController"
    }

    private var midiDevice: MidiDevice? = null
    private var midiOutputPort: MidiOutputPort? = null
    private var midiCallback: ((status: Int, data1: Int, data2: Int) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Connect to a Bluetooth MIDI device
     */
    @SuppressLint("MissingPermission")
    fun connect(
        device: BluetoothDevice,
        midiManager: MidiManager,
        onResult: (MidiDevice?, Boolean) -> Unit
    ) {
        Log.d(TAG, "Connecting to MIDI device: ${device.name}")

        midiManager.openBluetoothDevice(
            device,
            { midiDevice ->
                if (midiDevice != null) {
                    this.midiDevice = midiDevice
                    setupMidiInput(midiDevice)
                    Log.d(TAG, "MIDI device opened successfully")
                    onResult(midiDevice, true)
                } else {
                    Log.e(TAG, "Failed to open MIDI device")
                    onResult(null, false)
                }
            },
            handler
        )
    }

    /**
     * Connect to a USB or virtual MIDI device
     */
    fun connect(deviceInfo: MidiDeviceInfo, onResult: (MidiDevice?, Boolean) -> Unit) {
        Log.d(TAG, "Connecting to MIDI device: ${deviceInfo.properties}")

        midiManager.openDevice(
            deviceInfo,
            { midiDevice ->
                if (midiDevice != null) {
                    this.midiDevice = midiDevice
                    setupMidiInput(midiDevice)
                    Log.d(TAG, "MIDI device opened successfully")
                    onResult(midiDevice, true)
                } else {
                    Log.e(TAG, "Failed to open MIDI device")
                    onResult(null, false)
                }
            },
            handler
        )
    }

    /**
     * Setup MIDI input (receive messages from device)
     */
    private fun setupMidiInput(device: MidiDevice) {
        val info = device.info
        val outputPorts = info.outputPortCount
        
        Log.d(TAG, "Device has $outputPorts output ports")

        if (outputPorts > 0) {
            midiOutputPort = device.openOutputPort(0)
            midiOutputPort?.connect(midiReceiver)
            Log.d(TAG, "Connected to output port 0")
        }
    }

    /**
     * Set callback for MIDI messages
     */
    fun setMidiCallback(callback: (status: Int, data1: Int, data2: Int) -> Unit) {
        this.midiCallback = callback
    }

    /**
     * MIDI receiver for incoming messages
     */
    private val midiReceiver = object : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            if (BuildConfig.DEBUG) {
                val hexData = data.sliceArray(offset until offset + count)
                    .joinToString(" ") { "%02X".format(it) }
                Log.v(TAG, "MIDI In Raw: $hexData")
            }

            // Parse MIDI messages
            var i = offset
            while (i < offset + count) {
                val status = data[i].toInt() and 0xFF
                
                when {
                    // Note Off: 0x80-0x8F
                    status in 0x80..0x8F && i + 2 < offset + count -> {
                        val note = data[i + 1].toInt() and 0x7F
                        val velocity = data[i + 2].toInt() and 0x7F
                        if (BuildConfig.DEBUG) Log.d(TAG, "Parsed Note Off: $note")
                        midiCallback?.invoke(status, note, velocity)
                        i += 3
                    }
                    // Note On: 0x90-0x9F
                    status in 0x90..0x9F && i + 2 < offset + count -> {
                        val note = data[i + 1].toInt() and 0x7F
                        val velocity = data[i + 2].toInt() and 0x7F
                        if (BuildConfig.DEBUG) Log.d(TAG, "Parsed Note On: $note, vel: $velocity")
                        midiCallback?.invoke(status, note, velocity)
                        i += 3
                    }
                    // Control Change: 0xB0-0xBF
                    status in 0xB0..0xBF && i + 2 < offset + count -> {
                        val controller = data[i + 1].toInt() and 0x7F
                        val value = data[i + 2].toInt() and 0x7F
                        if (BuildConfig.DEBUG) Log.d(TAG, "Parsed CC: ctrl=$controller, val=$value")
                        midiCallback?.invoke(status, controller, value)
                        i += 3
                    }
                    // Program Change: 0xC0-0xCF
                    status in 0xC0..0xCF && i + 1 < offset + count -> {
                        val program = data[i + 1].toInt() and 0x7F
                        if (BuildConfig.DEBUG) Log.d(TAG, "Parsed Program Change: $program")
                        midiCallback?.invoke(status, program, 0)
                        i += 2
                    }
                    // Pitch Bend: 0xE0-0xEF
                    status in 0xE0..0xEF && i + 2 < offset + count -> {
                        val lsb = data[i + 1].toInt() and 0x7F
                        val msb = data[i + 2].toInt() and 0x7F
                        if (BuildConfig.DEBUG) Log.d(TAG, "Parsed Pitch Bend: lsb=$lsb, msb=$msb")
                        midiCallback?.invoke(status, lsb, msb)
                        i += 3
                    }
                    else -> i++
                }
            }
        }
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() {
        try {
            midiOutputPort?.disconnect(midiReceiver)
            midiOutputPort?.close()
            midiDevice?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        } finally {
            midiOutputPort = null
            midiDevice = null
            Log.d(TAG, "Disconnected from MIDI device")
        }
    }

    /**
     * Check if connected to a device
     */
    fun isConnected(): Boolean = midiDevice != null
}
