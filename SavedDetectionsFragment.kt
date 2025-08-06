package org.tensorflow.lite.examples.objectdetection.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import org.tensorflow.lite.examples.objectdetection.adapter.SavedDetectionsAdapter
import org.tensorflow.lite.examples.objectdetection.database.DetectionDatabase
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentSavedDetectionsBinding
import org.tensorflow.lite.examples.objectdetection.viewmodel.DetectionViewModel
import org.tensorflow.lite.examples.objectdetection.viewmodel.DetectionViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SavedDetectionsFragment : Fragment() {

    private var _binding: FragmentSavedDetectionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SavedDetectionsAdapter
    private lateinit var viewModel: DetectionViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedDetectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dao = DetectionDatabase.getDatabase(requireContext()).detectionDao()
        val factory = DetectionViewModelFactory(dao)
        viewModel = viewModels<DetectionViewModel> { factory }.value

        adapter = SavedDetectionsAdapter(mutableListOf(), onDeleteClick = { detection ->
            CoroutineScope(Dispatchers.IO).launch {
                dao.deleteDetection(detection)
            }
        })

        binding.recyclerViewDetections.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewDetections.adapter = adapter

        // Toolbar back button
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }


        // Observasi data dari Room
        viewModel.allDetections.observe(viewLifecycleOwner, Observer { detections ->
            adapter.updateList(detections)
            binding.textNoData.visibility = if (detections.isEmpty()) View.VISIBLE else View.GONE
        })

        // Hapus semua deteksi
        binding.buttonDeleteAll.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val deletedCount = dao.deleteAllDetections()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "$deletedCount data dihapus", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
