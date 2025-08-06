package org.tensorflow.lite.examples.objectdetection.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.objectdetection.model.Detection
import org.tensorflow.lite.examples.objectdetection.databinding.ItemDetectionBinding

class DetectionAdapter(
    private val onItemClick: (Detection) -> Unit,
    private val onDeleteClick: (Detection) -> Unit
) : ListAdapter<Detection, DetectionAdapter.DetectionViewHolder>(DiffCallback()) {

    class DetectionViewHolder(
        private val binding: ItemDetectionBinding,
        private val onItemClick: (Detection) -> Unit,
        private val onDeleteClick: (Detection) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(detection: Detection) {
            binding.apply {
                labelTextView.text = detection.label
                confidenceText.text = "${String.format("%.1f", detection.confidence * 100)}%"
                dateText.text = detection.dateString

                // Set warna berdasarkan label
                val color = when (detection.label) {
                    "Sehat" -> ContextCompat.getColor(root.context, android.R.color.holo_green_dark)
                    "Kurang_Air" -> ContextCompat.getColor(root.context, android.R.color.holo_blue_dark)
                    "Kalium" -> ContextCompat.getColor(root.context, android.R.color.holo_orange_dark)
                    "Kalsium" -> ContextCompat.getColor(root.context, android.R.color.holo_purple)
                    "Nitrogen" -> ContextCompat.getColor(root.context, android.R.color.holo_blue_light)
                    else -> ContextCompat.getColor(root.context, android.R.color.black)
                }

                labelTextView.setTextColor(color)

                // Set icon berdasarkan label dengan fallback ke drawable default
                val iconRes = when (detection.label) {
                    "Sehat" -> getDrawableResource("ic_check_circle")
                    "Kurang_Air" -> getDrawableResource("ic_water_drop")
                    "Kalium" -> getDrawableResource("ic_agriculture")
                    "Kalsium" -> getDrawableResource("ic_science")
                    "Nitrogen" -> getDrawableResource("ic_eco")
                    else -> getDrawableResource("ic_help")
                }

                iconImageView.setImageResource(iconRes)

                // Click listeners
                root.setOnClickListener { onItemClick(detection) }
                deleteButton.setOnClickListener { onDeleteClick(detection) }
            }
        }

        private fun getDrawableResource(iconName: String): Int {
            val context = binding.root.context
            val resourceId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            return if (resourceId != 0) resourceId else android.R.drawable.ic_dialog_info
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetectionViewHolder {
        val binding = ItemDetectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DetectionViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: DetectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Detection>() {
        override fun areItemsTheSame(oldItem: Detection, newItem: Detection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Detection, newItem: Detection): Boolean {
            return oldItem == newItem
        }
    }
}