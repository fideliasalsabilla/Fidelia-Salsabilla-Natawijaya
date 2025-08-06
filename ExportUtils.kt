package org.tensorflow.lite.examples.objectdetection.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import org.tensorflow.lite.examples.objectdetection.model.Detection
import java.io.File

object ExportUtils {

    fun exportAndShare(context: Context, detections: List<Detection>, format: ExportFormat) {
        if (detections.isEmpty()) {
            Toast.makeText(context, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }

        val file = when (format) {
            ExportFormat.JSON -> FileUtils.exportDetectionsToJson(context, detections)
            ExportFormat.CSV -> FileUtils.exportDetectionsToCsv(context, detections)
        }

        if (file != null) {
            shareFile(context, file)
            Toast.makeText(context, "File berhasil diekspor: ${file.name}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Gagal mengekspor file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (file.extension == "json") "application/json" else "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Data Deteksi")
                putExtra(Intent.EXTRA_TEXT, "File hasil deteksi dari aplikasi")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Bagikan file"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan file", Toast.LENGTH_SHORT).show()
        }
    }

    enum class ExportFormat {
        JSON, CSV
    }
}