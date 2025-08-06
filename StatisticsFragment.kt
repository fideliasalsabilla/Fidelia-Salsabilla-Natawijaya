package org.tensorflow.lite.examples.objectdetection.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentStatisticsBinding
import org.tensorflow.lite.examples.objectdetection.viewmodels.DetectionViewModel

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DetectionViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[DetectionViewModel::class.java]

        loadStatistics()
    }

    private fun loadStatistics() {
        val labels = listOf("Sehat", "Kurang_Air", "Kalium", "Kalsium", "Nitrogen")

        labels.forEach { label ->
            viewModel.countByLabel(label).observe(viewLifecycleOwner) { count ->
                when (label) {
                    "Sehat" -> binding.sehatCount.text = count.toString()
                    "Kurang_Air" -> binding.kurangAirCount.text = count.toString()
                    "Kalium" -> binding.kaliumCount.text = count.toString()
                    "Kalsium" -> binding.kalsiumCount.text = count.toString()
                    "Nitrogen" -> binding.nitrogenCount.text = count.toString()
                }
            }
        }

        viewModel.count().observe(viewLifecycleOwner) { totalCount ->
            binding.totalCount.text = totalCount.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}