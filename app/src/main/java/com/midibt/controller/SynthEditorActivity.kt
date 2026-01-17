package com.midibt.controller

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.midibt.controller.databinding.ActivitySynthEditorBinding
import com.midibt.controller.midi.MidiController
import com.midibt.controller.synth.SynthEngine

class SynthEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySynthEditorBinding
    private lateinit var synthEngine: SynthEngine
    private lateinit var midiController: MidiController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySynthEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        synthEngine = SynthEngine.getInstance(this)
        midiController = MidiController.getInstance(this)

        setupUIListeners()
        setupMidiRemoteControl()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupUIListeners() {
        // Enviar cambios manuales al sintetizador usando códigos estándar
        binding.sbCutoff.setOnSeekBarChangeListener(createCCListener(74))
        binding.sbResonance.setOnSeekBarChangeListener(createCCListener(71))
        binding.sbAttack.setOnSeekBarChangeListener(createCCListener(73))
        binding.sbDecay.setOnSeekBarChangeListener(createCCListener(75))
        binding.sbSustain.setOnSeekBarChangeListener(createCCListener(79))
        binding.sbRelease.setOnSeekBarChangeListener(createCCListener(72))
        binding.sbReverb.setOnSeekBarChangeListener(createCCListener(91))
        binding.sbChorus.setOnSeekBarChangeListener(createCCListener(93))
    }

    private fun createCCListener(cc: Int) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
            if (fromUser) synthEngine.controlChange(0, cc, p)
        }
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    }

    private fun setupMidiRemoteControl() {
        midiController.setMidiCallback { status, data1, data2 ->
            val type = status and 0xF0
            val channel = status and 0x0F

            if (type == 0xB0) {
                var targetStandardCC = -1
                
                runOnUiThread {
                    // TRADUCCIÓN: RockJam (70-77) -> Estándar MIDI
                    when (data1) {
                        70 -> { // Perilla 1
                            binding.sbCutoff.progress = data2
                            targetStandardCC = 74 // Cutoff estándar
                        }
                        71 -> { // Perilla 2
                            binding.sbResonance.progress = data2
                            targetStandardCC = 71 // Resonance estándar
                        }
                        72 -> { // Perilla 3
                            binding.sbRelease.progress = data2
                            targetStandardCC = 72
                        }
                        73 -> { // Perilla 4
                            binding.sbAttack.progress = data2
                            targetStandardCC = 73
                        }
                        75 -> { // Perilla 5
                            binding.sbDecay.progress = data2
                            targetStandardCC = 75
                        }
                        76 -> { // Perilla 6
                            binding.sbSustain.progress = data2
                            targetStandardCC = 79
                        }
                        77 -> { // Perilla 7
                            binding.sbChorus.progress = data2
                            targetStandardCC = 93
                        }
                    }
                }
                
                // Si la perilla física movió un control, enviamos la traducción al sintetizador
                if (targetStandardCC != -1) {
                    synthEngine.controlChange(channel, targetStandardCC, data2)
                }
            }
            
            // Reenvío de notas
            if (type == 0x90) {
                if (data2 > 0) synthEngine.noteOn(channel, data1, data2)
                else synthEngine.noteOff(channel, data1)
            } else if (type == 0x80) {
                synthEngine.noteOff(channel, data1)
            }
        }
    }
}
