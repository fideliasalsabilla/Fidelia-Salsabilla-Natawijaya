package org.tensorflow.lite.examples.objectdetection.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.objectdetection.databinding.ItemDetectionInfoBinding
import org.tensorflow.lite.examples.objectdetection.model.Detection
import java.text.SimpleDateFormat
import java.util.*

class SavedDetectionAdapter(
    private val items: List<Detection>,
    private val onDeleteClick: (Detection) -> Unit
) : RecyclerView.Adapter<SavedDetectionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDetectionInfoBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetectionInfoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.labelTextView.text = item.label
        holder.binding.infoTextView.text = item.info
        holder.binding.timestampTextView.text = formatTimestamp(item.timestamp)

        // ✅ Tombol Hapus
        holder.binding.deleteButton.setOnClickListener {
            onDeleteClick(item)
        }
    }

    private fun formatTimestamp(timestamp: Long?): String {
        timestamp ?: return "-"
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
