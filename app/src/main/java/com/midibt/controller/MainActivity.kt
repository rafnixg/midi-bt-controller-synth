package com.midibt.controller

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.midibt.controller.databinding.ActivityMainBinding
import com.midibt.controller.midi.BluetoothMidiScanner
import com.midibt.controller.midi.MidiController
import com.midibt.controller.synth.SoundBank
import com.midibt.controller.synth.SynthEngine
import com.midibt.controller.ui.DeviceAdapter
import com.midibt.controller.ui.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var midiManager: MidiManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var midiScanner: BluetoothMidiScanner
    private lateinit var midiController: MidiController
    private lateinit var synthEngine: SynthEngine
    private lateinit var deviceAdapter: DeviceAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var allInstruments = listOf<SoundBank>()
    private var percussionSets = listOf<SoundBank>()
    private val categories = listOf(
        "Piano", "Chromatic Percussion", "Organ", "Guitar", 
        "Bass", "Strings", "Ensemble", "Brass", 
        "Reed", "Pipe", "Synth Lead", "Synth Pad", 
        "Synth Effects", "Ethnic", "Percussive", "Sound Effects"
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) startBluetoothScan()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        initializeServices()
        setupUI()
        setupRecyclerViews()
        setupDropdowns()
        observeViewModel()
        initializeSynthEngine()
    }

    private fun initializeServices() {
        midiManager = getSystemService(Context.MIDI_SERVICE) as MidiManager
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        midiScanner = BluetoothMidiScanner(this, bluetoothAdapter)
        midiController = MidiController(midiManager)
        synthEngine = SynthEngine(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    private fun setupUI() {
        binding.btnScan.setOnClickListener {
            if (midiScanner.isScanning) stopBluetoothScan() else checkPermissionsAndScan()
        }
    }

    private fun setupRecyclerViews() {
        deviceAdapter = DeviceAdapter { connectToDevice(it) }
        binding.devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }
    }

    private fun setupDropdowns() {
        // Cargar todos los instrumentos GM y percusión
        loadAllInstruments()
        loadPercussionSets()

        // Adapter para Categorías del Teclado
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        binding.categoryDropdown.setAdapter(categoryAdapter)

        binding.categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categories[position]
            updateInstrumentDropdown(selectedCategory)
        }

        // Adapter para Sets de Percusión (Pads)
        val percNames = percussionSets.map { it.name }
        val percAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, percNames)
        binding.percussionDropdown.setAdapter(percAdapter)
        
        binding.percussionDropdown.setOnItemClickListener { _, _, position, _ ->
            val set = percussionSets[position]
            // Cambiamos el programa en el canal 9 (Canal 10 MIDI real para percusión)
            synthEngine.programChange(9, set.program)
            Toast.makeText(this, "Set Percusión: ${set.name}", Toast.LENGTH_SHORT).show()
        }

        // Valores por defecto
        binding.categoryDropdown.setText(categories[0], false)
        updateInstrumentDropdown(categories[0])
        
        if (percussionSets.isNotEmpty()) {
            binding.percussionDropdown.setText(percussionSets[0].name, false)
            // No llamamos a programChange aquí para no saturar al inicio, 
            // pero el canal 9 ya está configurado para percusión por defecto en el motor.
        }
    }

    private fun updateInstrumentDropdown(category: String) {
        val filteredInstruments = allInstruments.filter { it.category == category }
        val instrumentNames = filteredInstruments.map { it.name }
        
        val instrumentAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, instrumentNames)
        binding.instrumentDropdown.setAdapter(instrumentAdapter)
        
        binding.instrumentDropdown.setOnItemClickListener { _, _, position, _ ->
            selectSoundBank(filteredInstruments[position])
        }

        // Seleccionar el primero de la lista
        if (filteredInstruments.isNotEmpty()) {
            binding.instrumentDropdown.setText(filteredInstruments[0].name, false)
            selectSoundBank(filteredInstruments[0])
        }
    }

    private fun observeViewModel() {
        viewModel.connectionState.observe(this) { updateConnectionUI(it) }
        viewModel.discoveredDevices.observe(this) { 
            deviceAdapter.submitList(it)
            binding.devicesCard.isVisible = it.isNotEmpty()
        }
        viewModel.isScanning.observe(this) { isScanning ->
            binding.btnScan.text = if (isScanning) "STOP SCAN" else "SCAN DEVICES"
        }
        viewModel.currentSoundBank.observe(this) { 
            binding.currentPresetText.text = "Preset: ${it?.name ?: "None"}"
        }
        viewModel.isLoading.observe(this) { binding.loadingOverlay.isVisible = it }
    }

    private fun checkPermissionsAndScan() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startBluetoothScan()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun startBluetoothScan() {
        if (!bluetoothAdapter.isEnabled) return
        viewModel.setScanning(true)
        viewModel.clearDevices()
        midiScanner.startScan { device -> runOnUiThread { viewModel.addDevice(device) } }
        handler.postDelayed({ stopBluetoothScan() }, 10000)
    }

    private fun stopBluetoothScan() {
        midiScanner.stopScan()
        viewModel.setScanning(false)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        viewModel.setLoading(true)
        stopBluetoothScan()
        midiController.connect(device, midiManager) { midiDevice, success ->
            runOnUiThread {
                viewModel.setLoading(false)
                if (success && midiDevice != null) {
                    viewModel.setConnected(device.name ?: "MIDI Device")
                    setupMidiReceiver()
                    Toast.makeText(this, "Conectado a ${device.name}", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.setDisconnected()
                }
            }
        }
    }

    private fun setupMidiReceiver() {
        midiController.setMidiCallback { status, data1, data2 ->
            val channel = status and 0x0F
            val type = status and 0xF0
            when (type) {
                0x90 -> if (data2 > 0) synthEngine.noteOn(channel, data1, data2) else synthEngine.noteOff(channel, data1)
                0x80 -> synthEngine.noteOff(channel, data1)
                0xB0 -> synthEngine.controlChange(channel, data1, data2)
            }
        }
    }

    private fun updateConnectionUI(state: MainViewModel.ConnectionState) {
        when (state) {
            is MainViewModel.ConnectionState.Connected -> {
                binding.connectionStatusText.text = "Connected to: ${state.deviceName}"
                binding.connectionIndicator.setBackgroundResource(R.drawable.circle_indicator_connected)
            }
            MainViewModel.ConnectionState.Disconnected -> {
                binding.connectionStatusText.text = "Not Connected"
                binding.connectionIndicator.setBackgroundResource(R.drawable.circle_indicator)
            }
            MainViewModel.ConnectionState.Connecting -> {
                binding.connectionStatusText.text = "Scanning..."
            }
        }
    }

    private fun loadPercussionSets() {
        percussionSets = listOf(
            SoundBank(0, "Standard Kit", "Drum", 0),
            SoundBank(8, "Room Kit", "Drum", 8),
            SoundBank(16, "Power Kit", "Drum", 16),
            SoundBank(24, "Electronic Kit", "Drum", 24),
            SoundBank(25, "TR-808/909 Kit", "Drum", 25),
            SoundBank(26, "Dance Kit", "Drum", 26),
            SoundBank(32, "Jazz Kit", "Drum", 32),
            SoundBank(40, "Brush Kit", "Drum", 40),
            SoundBank(48, "Orchestra Kit", "Drum", 48),
            SoundBank(56, "SFX Kit", "Drum", 56)
        )
    }

    private fun loadAllInstruments() {
        val list = mutableListOf<SoundBank>()
        // 0-7 Piano
        val pianos = listOf("Acoustic Grand Piano", "Bright Acoustic Piano", "Electric Grand Piano", "Honky-tonk Piano", "Electric Piano 1", "Electric Piano 2", "Harpsichord", "Clavinet")
        pianos.forEachIndexed { i, s -> list.add(SoundBank(i, s, "Piano", i)) }
        // 8-15 Chromatic Percussion
        val chrom = listOf("Celesta", "Glockenspiel", "Music Box", "Vibraphone", "Marimba", "Xylophone", "Tubular Bells", "Dulcimer")
        chrom.forEachIndexed { i, s -> list.add(SoundBank(i+8, s, "Chromatic Percussion", i+8)) }
        // 16-23 Organ
        val organs = listOf("Drawbar Organ", "Percussive Organ", "Rock Organ", "Church Organ", "Reed Organ", "Accordion", "Harmonica", "Tango Accordion")
        organs.forEachIndexed { i, s -> list.add(SoundBank(i+16, s, "Organ", i+16)) }
        // 24-31 Guitar
        val guitars = listOf("Acoustic Guitar (nylon)", "Acoustic Guitar (steel)", "Electric Guitar (jazz)", "Electric Guitar (clean)", "Electric Guitar (muted)", "Overdriven Guitar", "Distortion Guitar", "Guitar harmonics")
        guitars.forEachIndexed { i, s -> list.add(SoundBank(i+24, s, "Guitar", i+24)) }
        // 32-39 Bass
        val basses = listOf("Acoustic Bass", "Electric Bass (finger)", "Electric Bass (pick)", "Fretless Bass", "Slap Bass 1", "Slap Bass 2", "Synth Bass 1", "Synth Bass 2")
        basses.forEachIndexed { i, s -> list.add(SoundBank(i+32, s, "Bass", i+32)) }
        // 40-47 Strings
        val strings = listOf("Violin", "Viola", "Cello", "Contrabass", "Tremolo Strings", "Pizzicato Strings", "Orchestral Harp", "Timpani")
        strings.forEachIndexed { i, s -> list.add(SoundBank(i+40, s, "Strings", i+40)) }
        // 48-55 Ensemble
        val ensemble = listOf("String Ensemble 1", "String Ensemble 2", "SynthStrings 1", "SynthStrings 2", "Choir Aahs", "Voice Oohs", "Synth Voice", "Orchestra Hit")
        ensemble.forEachIndexed { i, s -> list.add(SoundBank(i+48, s, "Ensemble", i+48)) }
        // 56-63 Brass
        val brass = listOf("Trumpet", "Trombone", "Tuba", "Muted Trumpet", "French Horn", "Brass Section", "SynthBrass 1", "SynthBrass 2")
        brass.forEachIndexed { i, s -> list.add(SoundBank(i+56, s, "Brass", i+56)) }
        // 64-71 Reed
        val reed = listOf("Soprano Sax", "Alto Sax", "Tenor Sax", "Baritone Sax", "Oboe", "English Horn", "Bassoon", "Clarinet")
        reed.forEachIndexed { i, s -> list.add(SoundBank(i+64, s, "Reed", i+64)) }
        // 72-79 Pipe
        val pipe = listOf("Piccolo", "Flute", "Recorder", "Pan Flute", "Blown Bottle", "Shakuhachi", "Whistle", "Ocarina")
        pipe.forEachIndexed { i, s -> list.add(SoundBank(i+72, s, "Pipe", i+72)) }
        // 80-87 Synth Lead
        val lead = listOf("Lead 1 (square)", "Lead 2 (sawtooth)", "Lead 3 (calliope)", "Lead 4 (chiff)", "Lead 5 (charang)", "Lead 6 (voice)", "Lead 7 (fifths)", "Lead 8 (bass + lead)")
        lead.forEachIndexed { i, s -> list.add(SoundBank(i+80, s, "Synth Lead", i+80)) }
        // 88-95 Synth Pad
        val pad = listOf("Pad 1 (new age)", "Pad 2 (warm)", "Pad 3 (polysynth)", "Pad 4 (choir)", "Pad 5 (bowed)", "Pad 6 (metallic)", "Pad 7 (halo)", "Pad 8 (sweep)")
        pad.forEachIndexed { i, s -> list.add(SoundBank(i+88, s, "Synth Pad", i+88)) }
        // 96-103 Synth Effects
        val sfx = listOf("FX 1 (rain)", "FX 2 (soundtrack)", "FX 3 (crystal)", "FX 4 (atmosphere)", "FX 5 (brightness)", "FX 6 (goblins)", "FX 7 (echoes)", "FX 8 (sci-fi)")
        sfx.forEachIndexed { i, s -> list.add(SoundBank(i+96, s, "Synth Effects", i+96)) }
        // 104-111 Ethnic
        val ethnic = listOf("Sitar", "Banjo", "Shamisen", "Koto", "Kalimba", "Bag pipe", "Fiddle", "Shanai")
        ethnic.forEachIndexed { i, s -> list.add(SoundBank(i+104, s, "Ethnic", i+104)) }
        // 112-119 Percussive
        val percussive = listOf("Tinkle Bell", "Agogo", "Steel Drums", "Woodblock", "Taiko Drum", "Melodic Tom", "Synth Drum", "Reverse Cymbal")
        percussive.forEachIndexed { i, s -> list.add(SoundBank(i+112, s, "Percussive", i+112)) }
        // 120-127 Sound Effects
        val soundEffects = listOf("Guitar Fret Noise", "Breath Noise", "Seashore", "Bird Tweet", "Telephone Ring", "Helicopter", "Applause", "Gunshot")
        soundEffects.forEachIndexed { i, s -> list.add(SoundBank(i+120, s, "Sound Effects", i+120)) }
        
        allInstruments = list
    }

    private fun selectSoundBank(soundBank: SoundBank) {
        viewModel.setCurrentSoundBank(soundBank)
        synthEngine.programChange(0, soundBank.program)
    }

    private fun initializeSynthEngine() {
        viewModel.setLoading(true)
        Thread {
            val success = synthEngine.initialize()
            runOnUiThread {
                viewModel.setLoading(false)
                if (!success) Toast.makeText(this, "Error Synth", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        midiScanner.stopScan()
        midiController.disconnect()
        synthEngine.shutdown()
    }
}
