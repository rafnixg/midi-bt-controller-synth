package com.midibt.controller

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.midibt.controller.databinding.ActivitySynthEditorBinding
import com.midibt.controller.midi.MidiController
import com.midibt.controller.synth.SoundBank
import com.midibt.controller.synth.SynthEngine

class SynthEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySynthEditorBinding
    private lateinit var synthEngine: SynthEngine
    private lateinit var midiController: MidiController

    private val waveForms = listOf(
        SoundBank(80, "Seno (Sine Wave)", "Wave", 80),
        SoundBank(81, "Sierra (Saw Wave)", "Wave", 81),
        SoundBank(82, "Cuadrada (Square)", "Wave", 80),
        SoundBank(118, "Pulso 808 (808 Tom)", "Wave", 118)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySynthEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        synthEngine = SynthEngine.getInstance(this)
        midiController = MidiController.getInstance(this)

        // ACTIVAR OSCILOSCOPIO
        synthEngine.setOscilloscopeCallback { audioData ->
            binding.oscilloscope.updateData(audioData)
        }

        setupWaveSelector()
        setupUIListeners()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupWaveSelector() {
        val names = waveForms.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        binding.waveDropdown.setAdapter(adapter)
        
        binding.waveDropdown.setOnItemClickListener { _, _, position, _ ->
            val wave = waveForms[position]
            synthEngine.bankSelect(0, 8)
            synthEngine.programChange(0, wave.program)
        }

        binding.waveDropdown.setText(waveForms[1].name, false)
        synthEngine.bankSelect(0, 8)
        synthEngine.programChange(0, waveForms[1].program)
    }

    private fun setupUIListeners() {
        binding.sbCutoff.setOnSeekBarChangeListener(createCCListener(74))
        binding.sbAttack.setOnSeekBarChangeListener(createCCListener(73))
        binding.sbRelease.setOnSeekBarChangeListener(createCCListener(72))
    }

    private fun createCCListener(cc: Int) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
            if (fromUser) synthEngine.controlChange(0, cc, p)
        }
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    }

    override fun onResume() {
        super.onResume()
        midiController.addMidiListener("editor") { status, data1, data2 ->
            val type = status and 0xF0
            val channel = status and 0x0F
            when (type) {
                0xB0 -> {
                    var targetCC = -1
                    when (data1) {
                        70 -> { targetCC = 74; runOnUiThread { binding.sbCutoff.progress = data2 } }
                        72 -> { targetCC = 72; runOnUiThread { binding.sbRelease.progress = data2 } }
                        73 -> { targetCC = 73; runOnUiThread { binding.sbAttack.progress = data2 } }
                    }
                    if (targetCC != -1) synthEngine.controlChange(channel, targetCC, data2)
                }
                0x90 -> if (data2 > 0) synthEngine.noteOn(channel, data1, data2) else synthEngine.noteOff(channel, data1)
                0x80 -> synthEngine.noteOff(channel, data1)
                0xE0 -> synthEngine.pitchBend(channel, (data2 shl 7) or data1)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        midiController.removeMidiListener("editor")
    }

    override fun onDestroy() {
        super.onDestroy()
        synthEngine.setOscilloscopeCallback { }
    }
}
