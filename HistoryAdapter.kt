package org.tensorflow.lite.examples.objectdetection.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.tensorflow.lite.examples.objectdetection.R
import org.tensorflow.lite.examples.objectdetection.model.Detection
import org.tensorflow.lite.examples.objectdetection.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onDeleteClick: (Detection) -> Unit
) : ListAdapter<Detection, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(detection: Detection) {
            binding.textLabel.text = getLabelDisplayName(detection.label)
            binding.textConfidence.text = "Confidence: ${String.format("%.1f", detection.confidence * 100)}%"
            binding.textTimestamp.text = formatTimestamp(detection.timestamp)

            if (!detection.imagePath.isNullOrEmpty()) {
                Glide.with(binding.imageViewResult.context)
                    .load(detection.imagePath)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(binding.imageViewResult)
            } else {
                binding.imageViewResult.setImageResource(R.drawable.ic_launcher_foreground)
            }

            binding.textLabel.setTextColor(getLabelColor(detection.label))

            binding.btnDelete.setOnClickListener {
                onDeleteClick(detection)
            }
        }

        private fun getLabelDisplayName(label: String): String {
            return when (label) {
                "Sehat" -> "Sehat"
                "Kurang_Air" -> "Kurang Air"
                "Kalium" -> "Defisiensi Kalium"
                "Kalsium" -> "Defisiensi Kalsium"
                "Nitrogen" -> "Defisiensi Nitrogen"
                else -> label
            }
        }

        private fun getLabelColor(label: String): Int {
            val context = binding.root.context
            return when (label) {
                "Sehat" -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
                "Kurang_Air" -> ContextCompat.getColor(context, android.R.color.holo_blue_dark)
                "Kalium" -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                "Kalsium" -> ContextCompat.getColor(context, android.R.color.holo_purple)
                "Nitrogen" -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
                else -> ContextCompat.getColor(context, android.R.color.black)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Detection>() {
        override fun areItemsTheSame(oldItem: Detection, newItem: Detection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Detection, newItem: Detection): Boolean {
            return oldItem == newItem
        }
    }
}