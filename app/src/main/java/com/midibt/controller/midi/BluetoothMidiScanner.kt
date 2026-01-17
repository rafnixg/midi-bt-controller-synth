package com.midibt.controller.midi

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log

/**
 * Handles Bluetooth LE scanning for MIDI devices
 */
class BluetoothMidiScanner(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) {
    companion object {
        private const val TAG = "BluetoothMidiScanner"
        
        // Standard UUID for MIDI over BLE
        private const val MIDI_OVER_BTLE_UUID = "03B80E5A-EDE8-4B33-A751-6CE34EC4C700"
    }

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    
    var isScanning: Boolean = false
        private set

    /**
     * Start scanning for MIDI BLE devices
     */
    @SuppressLint("MissingPermission")
    fun startScan(onDeviceFound: (BluetoothDevice) -> Unit) {
        if (isScanning) {
            Log.w(TAG, "Already scanning")
            return
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner not available")
            return
        }

        // Create scan filter for MIDI BLE devices
        val midiUuid = ParcelUuid.fromString(MIDI_OVER_BTLE_UUID)
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(midiUuid)
            .build()

        // Also scan without filter to find more devices
        val scanFilters = listOf<ScanFilter>() // Empty = scan all BLE devices

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val deviceName = device.name
                
                // Filter for devices that might be MIDI controllers
                if (deviceName != null && isMidiDevice(result)) {
                    Log.d(TAG, "Found MIDI device: $deviceName (${device.address})")
                    onDeviceFound(device)
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                for (result in results) {
                    onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error: $errorCode")
                isScanning = false
            }
        }

        try {
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
            isScanning = true
            Log.d(TAG, "Started BLE scan for MIDI devices")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan: ${e.message}")
            isScanning = false
        }
    }

    /**
     * Stop scanning for devices
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return

        try {
            scanCallback?.let {
                bluetoothLeScanner?.stopScan(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}")
        } finally {
            isScanning = false
            scanCallback = null
            Log.d(TAG, "Stopped BLE scan")
        }
    }

    /**
     * Check if a device is likely a MIDI device
     */
    @SuppressLint("MissingPermission")
    private fun isMidiDevice(result: ScanResult): Boolean {
        val device = result.device
        val name = device.name?.lowercase() ?: return false
        
        // Check for MIDI service UUID in advertised services
        val serviceUuids = result.scanRecord?.serviceUuids
        if (serviceUuids != null) {
            for (uuid in serviceUuids) {
                if (uuid.toString().uppercase() == MIDI_OVER_BTLE_UUID.uppercase()) {
                    return true
                }
            }
        }

        // Check common MIDI keyboard/controller name patterns
        val midiKeywords = listOf(
            "midi", "keyboard", "piano", "synth", "controller",
            "akai", "arturia", "korg", "roland", "yamaha", "novation",
            "native instruments", "m-audio", "alesis", "launchpad",
            "mpk", "keystation", "keystep", "microkey"
        )

        return midiKeywords.any { keyword -> name.contains(keyword) }
    }
}
