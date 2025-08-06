package org.tensorflow.lite.examples.objectdetection.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.objectdetection.R
import org.tensorflow.lite.examples.objectdetection.model.Detection

class SavedDetectionsAdapter(
    private val detections: MutableList<Detection>,
    private val onDeleteClick: (Detection) -> Unit
) : RecyclerView.Adapter<SavedDetectionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelTextView: TextView = view.findViewById(R.id.labelTextView)
        val suggestionTextView: TextView = view.findViewById(R.id.suggestionTextView)
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
        val card: CardView = view.findViewById(R.id.cardItem)
    }

    fun removeItem(detection: Detection) {
        val index = detections.indexOfFirst { it.id == detection.id }
        if (index != -1) {
            detections.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updateList(newList: List<Detection>) {
        detections.clear()
        detections.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detection, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = detections.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = detections[position]
        holder.labelTextView.text = "Label: ${item.label}"
        holder.suggestionTextView.text = "Saran: ${item.suggestion}"
        holder.timestampTextView.text = "Waktu: ${item.timestamp}"

        // Klik untuk hapus data
        holder.card.setOnLongClickListener {
            onDeleteClick(item)
            true
        }
    }
}
