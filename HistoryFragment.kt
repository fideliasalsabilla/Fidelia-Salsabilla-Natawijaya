package org.tensorflow.lite.examples.objectdetection.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import org.tensorflow.lite.examples.objectdetection.adapters.HistoryAdapter
import org.tensorflow.lite.examples.objectdetection.model.Detection
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentHistoryBinding
import org.tensorflow.lite.examples.objectdetection.utils.ExportUtils
import org.tensorflow.lite.examples.objectdetection.viewmodels.DetectionViewModel

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DetectionViewModel
    private lateinit var historyAdapter: HistoryAdapter
    private var currentDetections: List<Detection> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[DetectionViewModel::class.java]

        setupRecyclerView()
        observeData()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { detection ->
            showDeleteConfirmationDialog(detection)
        }

        binding.recyclerViewHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeData() {
        viewModel.getAllResults().observe(viewLifecycleOwner) { results ->
            currentDetections = results
            if (results.isEmpty()) {
                binding.recyclerViewHistory.visibility = View.GONE
                binding.textEmptyHistory.visibility = View.VISIBLE
            } else {
                binding.recyclerViewHistory.visibility = View.VISIBLE
                binding.textEmptyHistory.visibility = View.GONE
                // Safely check if btnExport exists before accessing it
                historyAdapter.submitList(results)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnClearHistory.setOnClickListener {
            showClearAllConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog(detectionResult: Detection) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Item")
            .setMessage("Apakah Anda yakin ingin menghapus item ini?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteResult(detectionResult)
                Toast.makeText(requireContext(), "Item berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showClearAllConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Semua Riwayat")
            .setMessage("Apakah Anda yakin ingin menghapus semua riwayat deteksi?")
            .setPositiveButton("Hapus Semua") { _, _ ->
                viewModel.deleteAll()
                Toast.makeText(requireContext(), "Semua riwayat berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showExportDialog() {
        val options = arrayOf("Ekspor ke JSON", "Ekspor ke CSV")

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Format Ekspor")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> ExportUtils.exportAndShare(requireContext(), currentDetections, ExportUtils.ExportFormat.JSON)
                    1 -> ExportUtils.exportAndShare(requireContext(), currentDetections, ExportUtils.ExportFormat.CSV)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}