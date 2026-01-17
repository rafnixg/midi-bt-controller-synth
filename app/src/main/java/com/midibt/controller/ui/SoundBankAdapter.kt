package com.midibt.controller.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.midibt.controller.R
import com.midibt.controller.databinding.ItemSoundBankBinding
import com.midibt.controller.synth.SoundBank

class SoundBankAdapter(
    private val onSoundBankClick: (SoundBank) -> Unit
) : ListAdapter<SoundBank, SoundBankAdapter.ViewHolder>(SoundBankDiffCallback()) {

    private var selectedId: Int = -1

    fun setSelectedId(id: Int) {
        val oldSelected = selectedId
        selectedId = id
        
        // Notify changes for old and new selection
        currentList.forEachIndexed { index, soundBank ->
            if (soundBank.id == oldSelected || soundBank.id == id) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSoundBankBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), selectedId)
    }

    inner class ViewHolder(
        private val binding: ItemSoundBankBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(soundBank: SoundBank, selectedId: Int) {
            binding.soundBankName.text = soundBank.name
            binding.soundBankCategory.text = "${soundBank.category} • Program ${soundBank.program}"

            val isSelected = soundBank.id == selectedId
            binding.cardSoundBank.isChecked = isSelected

            // Set icon based on category
            val iconRes = when (soundBank.category) {
                "Piano" -> android.R.drawable.ic_media_play
                "Organ" -> android.R.drawable.ic_menu_compass
                "Guitar" -> android.R.drawable.ic_menu_gallery
                "Bass" -> android.R.drawable.ic_menu_sort_by_size
                "Strings" -> android.R.drawable.ic_menu_edit
                "Brass" -> android.R.drawable.ic_menu_call
                "Reed" -> android.R.drawable.ic_menu_view
                "Synth Lead", "Synth Pad" -> android.R.drawable.ic_menu_slideshow
                else -> android.R.drawable.ic_media_play
            }
            binding.iconSoundBank.setImageResource(iconRes)

            binding.root.setOnClickListener {
                onSoundBankClick(soundBank)
            }
        }
    }

    class SoundBankDiffCallback : DiffUtil.ItemCallback<SoundBank>() {
        override fun areItemsTheSame(oldItem: SoundBank, newItem: SoundBank): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SoundBank, newItem: SoundBank): Boolean {
            return oldItem == newItem
        }
    }
}
