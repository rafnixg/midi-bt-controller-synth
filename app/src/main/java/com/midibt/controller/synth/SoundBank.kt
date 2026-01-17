package com.midibt.controller.synth

/**
 * Represents a sound bank/preset in the General MIDI standard
 */
data class SoundBank(
    val id: Int,
    val name: String,
    val category: String,
    val program: Int,
    val bank: Int = 0
)
