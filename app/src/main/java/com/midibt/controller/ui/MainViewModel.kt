package com.midibt.controller.ui

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.midibt.controller.synth.SoundBank

class MainViewModel : ViewModel() {

    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data class Connected(val deviceName: String) : ConnectionState()
    }

    private val _connectionState = MutableLiveData<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _discoveredDevices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: LiveData<List<BluetoothDevice>> = _discoveredDevices

    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _currentSoundBank = MutableLiveData<SoundBank?>()
    val currentSoundBank: LiveData<SoundBank?> = _currentSoundBank

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val deviceList = mutableListOf<BluetoothDevice>()

    fun setConnected(deviceName: String) {
        _connectionState.value = ConnectionState.Connected(deviceName)
    }

    fun setDisconnected() {
        _connectionState.value = ConnectionState.Disconnected
    }

    fun setConnecting() {
        _connectionState.value = ConnectionState.Connecting
    }

    fun addDevice(device: BluetoothDevice) {
        if (deviceList.none { it.address == device.address }) {
            deviceList.add(device)
            _discoveredDevices.value = deviceList.toList()
        }
    }

    fun clearDevices() {
        deviceList.clear()
        _discoveredDevices.value = emptyList()
    }

    fun setScanning(scanning: Boolean) {
        _isScanning.value = scanning
        if (scanning) {
            setConnecting()
        }
    }

    fun setCurrentSoundBank(soundBank: SoundBank) {
        _currentSoundBank.value = soundBank
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}
