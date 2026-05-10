package com.midibt.controller

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
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
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var midiScanner: BluetoothMidiScanner
    private lateinit var midiController: MidiController
    private lateinit var synthEngine: SynthEngine
    private lateinit var deviceAdapter: DeviceAdapter

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val prefs by lazy { getSharedPreferences("midi_prefs", Context.MODE_PRIVATE) }
    private var allInstruments = mutableListOf<SoundBank>()
    private var percussionSets = listOf<SoundBank>()

    private val btDisconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED && midiController.isConnected) {
                midiController.notifyDisconnect()
            }
        }
    }
    
    private val categories = listOf(
        "Piano", "Chromatic Percussion", "Organ", "Guitar", 
        "Bass", "Strings", "Ensemble", "Brass", 
        "Reed", "Pipe", "Synth Lead", "Synth Pad", 
        "Synth Effects", "Ethnic", "Percussive", "Sound Effects"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        initializeServices()
        setupUI()
        setupRecyclerViews() // <--- Llamada que fallaba
        loadAllInstruments()
        loadPercussionSets()
        setupDropdowns()
        observeViewModel()
        initializeSynthEngine()
    }

    private fun initializeServices() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        midiScanner = BluetoothMidiScanner(this, bluetoothAdapter)
        midiController = MidiController.getInstance(this)
        synthEngine = SynthEngine.getInstance(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        registerReceiver(btDisconnectReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
    }

    private fun setupUI() {
        binding.btnScan.setOnClickListener {
            if (midiScanner.isScanning) stopBluetoothScan() else checkPermissionsAndScan()
        }
        binding.btnOpenEditor.setOnClickListener {
            startActivity(Intent(this, SynthEditorActivity::class.java))
        }
        binding.btnPanic.setOnClickListener { synthEngine.allNotesOff() }
    }

    private fun setupRecyclerViews() {
        deviceAdapter = DeviceAdapter { connectToDevice(it) }
        binding.devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        setupMidiReceiver()
    }

    override fun onPause() {
        super.onPause()
        midiController.removeMidiListener("main")
    }

    private fun setupMidiReceiver() {
        midiController.addMidiListener("main") { status, data1, data2 ->
            val channel = status and 0x0F
            val type = status and 0xF0
            when (type) {
                0x90 -> if (data2 > 0) synthEngine.noteOn(channel, data1, data2) else synthEngine.noteOff(channel, data1)
                0x80 -> synthEngine.noteOff(channel, data1)
                0xB0 -> synthEngine.controlChange(channel, data1, data2)
                0xE0 -> synthEngine.pitchBend(channel, (data2 shl 7) or data1)
                0xC0 -> {
                    synthEngine.programChange(channel, data1)
                    val match = allInstruments.firstOrNull { it.program == data1 }
                    if (match != null) runOnUiThread { selectSoundBank(match) }
                }
            }
        }
    }

    private fun setupDropdowns() {
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        binding.categoryDropdown.setAdapter(catAdapter)
        binding.categoryDropdown.setOnItemClickListener { _, _, position, _ -> updateInstrumentDropdown(categories[position]) }

        val percAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, percussionSets.map { it.name })
        binding.percussionDropdown.setAdapter(percAdapter)
        binding.percussionDropdown.setOnItemClickListener { _, _, position, _ ->
            synthEngine.programChange(9, percussionSets[position].program)
            prefs.edit().putInt("last_percussion", percussionSets[position].program).apply()
        }

        val savedCategory = prefs.getString("last_category", categories[0])
            ?.takeIf { it in categories } ?: categories[0]
        val savedInstrument = prefs.getString("last_instrument", null)
        val savedPercProgram = prefs.getInt("last_percussion", percussionSets[0].program)
        val savedPercIdx = percussionSets.indexOfFirst { it.program == savedPercProgram }.takeIf { it >= 0 } ?: 0

        binding.categoryDropdown.setText(savedCategory, false)
        binding.percussionDropdown.setText(percussionSets[savedPercIdx].name, false)
        synthEngine.programChange(9, percussionSets[savedPercIdx].program)
        updateInstrumentDropdown(savedCategory, savedInstrument)
    }

    private fun updateInstrumentDropdown(category: String, selectName: String? = null) {
        val filtered = allInstruments.filter { it.category == category }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filtered.map { it.name })
        binding.instrumentDropdown.setAdapter(adapter)
        binding.instrumentDropdown.setOnItemClickListener { _, _, pos, _ -> selectSoundBank(filtered[pos]) }
        if (filtered.isNotEmpty()) {
            val idx = selectName?.let { name -> filtered.indexOfFirst { it.name == name }.takeIf { it >= 0 } } ?: 0
            binding.instrumentDropdown.setText(filtered[idx].name, false)
            selectSoundBank(filtered[idx])
        }
    }

    private fun selectSoundBank(soundBank: SoundBank) {
        viewModel.setCurrentSoundBank(soundBank)
        synthEngine.programChange(0, soundBank.program)
        prefs.edit()
            .putString("last_category", soundBank.category)
            .putString("last_instrument", soundBank.name)
            .apply()
    }

    private fun loadAllInstruments() {
        allInstruments.clear()
        fun add(names: List<String>, cat: String, startIdx: Int) {
            names.forEachIndexed { i, name -> allInstruments.add(SoundBank(startIdx + i, name, cat, startIdx + i)) }
        }
        add(listOf("Acoustic Grand Piano", "Bright Acoustic Piano", "Electric Grand Piano", "Honky-tonk Piano", "Electric Piano 1", "Electric Piano 2", "Harpsichord", "Clavinet"), "Piano", 0)
        add(listOf("Celesta", "Glockenspiel", "Music Box", "Vibraphone", "Marimba", "Xylophone", "Tubular Bells", "Dulcimer"), "Chromatic Percussion", 8)
        add(listOf("Drawbar Organ", "Percussive Organ", "Rock Organ", "Church Organ", "Reed Organ", "Accordion", "Harmonica", "Tango Accordion"), "Organ", 16)
        add(listOf("Acoustic Guitar (nylon)", "Acoustic Guitar (steel)", "Electric Guitar (jazz)", "Electric Guitar (clean)", "Electric Guitar (muted)", "Overdriven Guitar", "Distortion Guitar", "Guitar harmonics"), "Guitar", 24)
        add(listOf("Acoustic Bass", "Electric Bass (finger)", "Electric Bass (pick)", "Fretless Bass", "Slap Bass 1", "Slap Bass 2", "Synth Bass 1", "Synth Bass 2"), "Bass", 32)
        add(listOf("Violin", "Viola", "Cello", "Contrabass", "Tremolo Strings", "Pizzicato Strings", "Orchestral Harp", "Timpani"), "Strings", 40)
        add(listOf("String Ensemble 1", "String Ensemble 2", "SynthStrings 1", "SynthStrings 2", "Choir Aahs", "Voice Oohs", "Synth Voice", "Orchestra Hit"), "Ensemble", 48)
        add(listOf("Trumpet", "Trombone", "Tuba", "Muted Trumpet", "French Horn", "Brass Section", "SynthBrass 1", "SynthBrass 2"), "Brass", 56)
        add(listOf("Soprano Sax", "Alto Sax", "Tenor Sax", "Baritone Sax", "Oboe", "English Horn", "Bassoon", "Clarinet"), "Reed", 64)
        add(listOf("Piccolo", "Flute", "Recorder", "Pan Flute", "Blown Bottle", "Shakuhachi", "Whistle", "Ocarina"), "Pipe", 72)
        add(listOf("Lead 1 (square)", "Lead 2 (sawtooth)", "Lead 3 (calliope)", "Lead 4 (chiff)", "Lead 5 (charang)", "Lead 6 (voice)", "Lead 7 (fifths)", "Lead 8 (bass + lead)"), "Synth Lead", 80)
        add(listOf("Pad 1 (new age)", "Pad 2 (warm)", "Pad 3 (polysynth)", "Pad 4 (choir)", "Pad 5 (bowed)", "Pad 6 (metallic)", "Pad 7 (halo)", "Pad 8 (sweep)"), "Synth Pad", 88)
        add(listOf("FX 1 (rain)", "FX 2 (soundtrack)", "FX 3 (crystal)", "FX 4 (atmosphere)", "FX 5 (brightness)", "FX 6 (goblins)", "FX 7 (echoes)", "FX 8 (sci-fi)"), "Synth Effects", 96)
        add(listOf("Sitar", "Banjo", "Shamisen", "Koto", "Kalimba", "Bag pipe", "Fiddle", "Shanai"), "Ethnic", 104)
        add(listOf("Tinkle Bell", "Agogo", "Steel Drums", "Woodblock", "Taiko Drum", "Melodic Tom", "Synth Drum", "Reverse Cymbal"), "Percussive", 112)
        add(listOf("Guitar Fret Noise", "Breath Noise", "Seashore", "Bird Tweet", "Telephone Ring", "Helicopter", "Applause", "Gunshot"), "Sound Effects", 120)
    }

    private fun loadPercussionSets() {
        percussionSets = listOf(SoundBank(0, "Standard Kit", "Drum", 0), SoundBank(8, "Room Kit", "Drum", 8), SoundBank(16, "Power Kit", "Drum", 16), SoundBank(25, "TR-808/909 Kit", "Drum", 25))
    }

    private fun checkPermissionsAndScan() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) startBluetoothScan()
        else registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { if (it.values.all { g -> g }) startBluetoothScan() }.launch(permissions)
    }

    private fun startBluetoothScan() {
        if (!bluetoothAdapter.isEnabled) return
        viewModel.setScanning(true)
        viewModel.clearDevices()
        midiScanner.startScan { device -> runOnUiThread { viewModel.addDevice(device) } }
        handler.postDelayed({ stopBluetoothScan() }, 10000)
    }

    private fun stopBluetoothScan() { midiScanner.stopScan(); viewModel.setScanning(false) }

    private fun connectToDevice(device: BluetoothDevice) {
        viewModel.setLoading(true)
        midiController.connect(device) { _, success ->
            runOnUiThread {
                viewModel.setLoading(false)
                if (success) {
                    val name = device.name ?: "MIDI"
                    viewModel.setConnected(name)
                    midiController.setDisconnectListener {
                        viewModel.setDisconnected()
                        Toast.makeText(this, "Dispositivo $name desconectado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.connectionState.observe(this) { updateConnectionUI(it) }
        viewModel.discoveredDevices.observe(this) { 
            deviceAdapter.submitList(it)
            binding.devicesCard.isVisible = it.isNotEmpty()
        }
        viewModel.isScanning.observe(this) { isScanning ->
            binding.btnScan.text = if (isScanning) "PARAR ESCANEO" else "BUSCAR DISPOSITIVOS"
        }
        viewModel.currentSoundBank.observe(this) { 
            binding.currentPresetText.text = "Preset: ${it?.name ?: "Ninguno"}"
        }
        viewModel.isLoading.observe(this) { binding.loadingOverlay.isVisible = it }
    }

    private fun updateConnectionUI(state: MainViewModel.ConnectionState) {
        val isConnected = state is MainViewModel.ConnectionState.Connected
        binding.connectionStatusText.text = if (isConnected) "Conectado" else "Desconectado"
        binding.connectionIndicator.setBackgroundResource(if (isConnected) R.drawable.circle_indicator_connected else R.drawable.circle_indicator)
    }

    private fun initializeSynthEngine() {
        viewModel.setLoading(true)
        Thread {
            synthEngine.initialize()
            runOnUiThread {
                viewModel.setLoading(false)
                requestAudioFocus()
            }
        }.start()
    }

    private fun requestAudioFocus() {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS ||
                    change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    synthEngine.allNotesOff()
                }
            }
            .build()
        audioManager.requestAudioFocus(audioFocusRequest!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        midiScanner.stopScan()
        unregisterReceiver(btDisconnectReceiver)
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }
}
