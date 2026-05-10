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
    private val midiListeners = LinkedHashMap<String, (Int, Int, Int) -> Unit>()
    private var disconnectListener: (() -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())

    val isConnected: Boolean get() = midiDevice != null

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

    /**
     * Registers a MIDI event listener identified by [tag].
     * Replaces any previous listener registered with the same tag.
     */
    fun addMidiListener(tag: String, callback: (status: Int, data1: Int, data2: Int) -> Unit) {
        synchronized(midiListeners) { midiListeners[tag] = callback }
        Log.d(TAG, "MIDI listener añadido: $tag (total: ${midiListeners.size})")
    }

    /** Removes the listener registered with [tag]. */
    fun removeMidiListener(tag: String) {
        synchronized(midiListeners) { midiListeners.remove(tag) }
        Log.d(TAG, "MIDI listener eliminado: $tag")
    }

    /** Sets a listener called when the Bluetooth device disconnects unexpectedly. */
    fun setDisconnectListener(listener: () -> Unit) {
        disconnectListener = listener
    }

    /**
     * Signals that the Bluetooth connection dropped unexpectedly.
     * Cleans up resources and notifies the disconnect listener on the main thread.
     */
    fun notifyDisconnect() {
        Log.w(TAG, "Dispositivo BT desconectado inesperadamente")
        disconnect()
        handler.post { disconnectListener?.invoke() }
    }

    private val midiReceiver = object : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            var i = offset
            while (i < offset + count) {
                val status = data[i].toInt() and 0xFF
                // Skip real-time messages (0xF8–0xFF): single-byte, no data
                if (status >= 0xF8) { i++; continue }
                // Skip SysEx / system common (0xF0–0xF7)
                if (status >= 0xF0) { i++; continue }

                val msgType = status and 0xF0
                // Program Change (0xC0) and Channel Pressure (0xD0): 1 data byte; all others: 2
                val msgLen = if (msgType == 0xC0 || msgType == 0xD0) 2 else 3

                if (i + msgLen <= offset + count) {
                    val d1 = data[i + 1].toInt() and 0x7F
                    val d2 = if (msgLen == 3) data[i + 2].toInt() and 0x7F else 0
                    Log.v(TAG, "MIDI IN: 0x${"%02X".format(status)} d1=$d1 d2=$d2")
                    val listeners = synchronized(midiListeners) { midiListeners.values.toList() }
                    listeners.forEach { it.invoke(status, d1, d2) }
                }
                i += msgLen
            }
        }
    }

    fun disconnect() {
        Log.w(TAG, "Desconectando...")
        midiOutputPort?.close()
        midiDevice?.close()
        midiOutputPort = null
        midiDevice = null
    }
}
