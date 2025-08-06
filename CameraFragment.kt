@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)
package org.tensorflow.lite.examples.objectdetection.fragments

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import android.widget.Toast
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentCameraBinding
import org.tensorflow.lite.examples.objectdetection.databinding.InfoBottomSheetBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.TextView
import org.tensorflow.lite.examples.objectdetection.R
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import android.Manifest
import androidx.core.app.ActivityCompat
import android.util.Size
import android.util.Log
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.widget.FrameLayout
import org.tensorflow.lite.examples.objectdetection.model.Detection
import com.google.firebase.database.FirebaseDatabase
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TensorFlow Lite imports
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.graphics.Matrix
import android.graphics.RectF
import org.tensorflow.lite.examples.objectdetection.utils.ImageUtils
import org.tensorflow.lite.examples.objectdetection.utils.DetectionResult
import androidx.lifecycle.lifecycleScope
import org.tensorflow.lite.examples.objectdetection.database.DetectionDatabase
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.objectdetection.model.LabelInfo

@Suppress("OptInUsageError")
@OptIn(androidx.camera.core.ExperimentalGetImage::class)
class CameraFragment : Fragment() {

    private var bitmapBuffer: Bitmap? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding: FragmentCameraBinding
        get() = _fragmentCameraBinding ?: throw IllegalStateException("View binding is not available")

    private var lastLabel: String? = null
    private var lastInfo: String? = null
    private val savedDetectionsList = mutableListOf<Detection>()
    private var currentDetection: DetectionResult? = null
    private var currentDetectionInfo: String? = null

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var latestDetections: List<DetectionResult> = emptyList()
    private var lastImageWidth: Int = 0
    private var lastImageHeight: Int = 0

    // TensorFlow Lite components
    private var interpreter: Interpreter? = null
    private var isInterpreterInitialized = false
    private var imageProcessor: ImageProcessor? = null
    private var inputImageWidth = 640
    private var inputImageHeight = 640
    private var outputArray: Array<Array<FloatArray>>? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val labelList = listOf("Kalium", "Kalsium", "Kurang_Air", "Nitrogen", "Sehat")

    // Detection result data class
    data class DetectionResultInternal(
        val x: Float, val y: Float, val w: Float, val h: Float,
        val score: Float, val classId: Int
    )

    private fun getRectFromDetection(d: DetectionResultInternal): RectF {
        val left = d.x - d.w / 2
        val top = d.y - d.h / 2
        val right = d.x + d.w / 2
        val bottom = d.y + d.h / 2
        return RectF(left, top, right, bottom)
    }

    private val labelInfoMap: Map<String, LabelInfo> = mapOf(
        "Sehat" to LabelInfo(
            description = """
        ✅ Tanaman sehat.
        • Tidak memerlukan perlakuan khusus saat ini.
        • Lanjutkan perawatan normal: penyiraman rutin & pengendalian hama.
        • Cek rutin kelembapan tanah dan kondisi daun.
        """.trimIndent(),
            suggestion = "Tanaman dalam kondisi sehat. Lanjutkan perawatan rutin."
        ),

        "Kurang_Air" to LabelInfo(
            description = """
        💧 Tanaman kekurangan air.
        • Solusi: Tingkatkan frekuensi penyiraman pagi & sore.
        • Gunakan mulsa (jerami, daun kering, atau serbuk gergaji) untuk menjaga kelembapan tanah.
        • Estimasi penambahan air: +20% dari dosis biasa.
        
        💡 Alternatif rumahan:
        • Gunakan botol plastik bekas (1,5 L), lubangi bagian bawah, isi air dan tanam di dekat akar sebagai irigasi tetes sederhana.
    """.trimIndent(),
            suggestion = "Tingkatkan penyiraman & gunakan mulsa untuk menjaga kelembapan."
        ),

        "Kalium" to LabelInfo(
            description = """
            ⚠ Kekurangan Kalium.
            • Solusi: Tambahkan pupuk Kalium.
            • Rekomendasi pupuk:
              - KCl (contoh: MOP Petrokimia, KCl Cap Kapal).
              - ZK (contoh: ZK Petro, Nitrofoska ZK).
            • Dosis: Tambahkan 20–30% pupuk Kalium dari dosis standar.
            • Manfaat: Memperkuat batang, mencegah buah busuk ujung, meningkatkan kualitas panen.
        """.trimIndent(),
            suggestion = """
            💡 Alternatif rumahan:
            • Potong kulit pisang, keringkan, lalu tanam di sekitar akar atau rendam selama 3 hari dan gunakan air rendamannya sebagai siraman.
        """.trimIndent()
        ),

        "Kalsium" to LabelInfo(
            description = """
            ⚠ Kekurangan Kalsium.
            • Solusi: Aplikasikan pupuk Kalsium.
            • Rekomendasi pupuk:
              - Kapur pertanian (contoh: Dolomit, Calcite Meroke).
              - Kalsium Nitrat [contoh: YaraLiva Tropicote, Cap Orang].
            • Dosis: Tambahkan 15–20% pupuk Kalsium dari dosis standar.
            • Manfaat: Mencegah busuk ujung buah & memperkuat dinding sel tanaman.
        """.trimIndent(),
            suggestion = """
            💡 Alternatif rumahan:
            • Haluskan kulit telur kering dan taburkan di sekitar tanaman untuk meningkatkan kadar Kalsium secara alami.
        """.trimIndent()
        ),

        "Nitrogen" to LabelInfo(
            description = """
            ⚠ Kekurangan Nitrogen.
            • Solusi: Tambahkan pupuk Nitrogen.
            • Rekomendasi pupuk:
              - Urea (contoh: Urea Pupuk Kujang, Daun Buaya).
              - ZA (contoh: ZA Petrokimia, ZA Meroke).
            • Dosis: Tambahkan 20–25% pupuk Nitrogen dari dosis standar.
            • Manfaat: Mendukung pertumbuhan daun & tunas baru, serta meningkatkan kehijauan daun.
        """.trimIndent(),
            suggestion = """
            💡 Alternatif rumahan:
            • Gunakan air cucian beras (diamkan semalam), lalu siramkan ke tanah untuk menambah unsur hara ringan secara alami.
        """.trimIndent()
        )
    )

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 10
        private const val TAG = "CameraFragment"
        private const val MODEL_PATH = "DeteksiCabai.tflite"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        val rootView = fragmentCameraBinding!!.root

        // Initialize TensorFlow Lite components
        initializeTensorFlowLite()

        // Setup komponen bottom sheet
        val bottomSheet = rootView.findViewById<View>(R.id.bottomSheet)
        val inferenceTimeVal = bottomSheet?.findViewById<TextView>(R.id.inferenceTimeVal)
        val rekomendasiTextView = bottomSheet?.findViewById<TextView>(R.id.rekomendasiTextView)
        val saveButton = bottomSheet?.findViewById<View>(R.id.saveButton)


        // Set initial text
        inferenceTimeVal?.text = "Inference Time: 0 ms"
        rekomendasiTextView?.text = "Arahkan kamera ke tanaman untuk deteksi..."

        // Debug: Cek apakah view ditemukan
        Log.d(TAG, "Bottom sheet found: ${bottomSheet != null}")
        Log.d(TAG, "rekomendasiTextView found: ${rekomendasiTextView != null}")
        Log.d(TAG, "inferenceTimeVal found: ${inferenceTimeVal != null}")

        saveButton?.setOnClickListener {
            saveCurrentDetection()
        }
        return rootView
    }

    private fun saveCurrentDetection() {
        try {
            // Cek apakah ada detection yang valid
            val detection = currentDetection
            val info = currentDetectionInfo
            val label = lastLabel

            Log.d(TAG, "🔥 [saveCurrentDetection] detection=$detection")
            Log.d(TAG, "🔥 [saveCurrentDetection] info=$info")
            Log.d(TAG, "🔥 [saveCurrentDetection] label=$label")

            if (detection != null && info != null && label != null) {
                Log.d(TAG, "Saving detection: $label with confidence: ${detection.confidence}")

                // Simpan ke Firebase
                saveDetectionToLocal(label, info, detection.confidence)
            } else {
                Log.w(TAG, "No valid detection to save")
                Toast.makeText(requireContext(), "Tidak ada deteksi yang dapat disimpan", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving current detection", e)
            Toast.makeText(requireContext(), "Error menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeTensorFlowLite() {
        try {
            // Load model
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options()
            options.setNumThreads(4)
            interpreter = Interpreter(modelBuffer, options)

            // Initialize image processor
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputImageWidth, inputImageHeight, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()

            isInterpreterInitialized = true
            Log.d(TAG, "TensorFlow Lite initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TensorFlow Lite", e)
            isInterpreterInitialized = false
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProvider = cameraProviderFuture.get()

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    fragmentCameraBinding?.let { binding ->
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, { imageProxy ->
                        processImageProxy(imageProxy)
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }


    @OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (!isInterpreterInitialized) {
            imageProxy.close()
            return
        }

        try {
            // Convert ImageProxy to Bitmap
            val bitmap = imageProxyToBitmap(imageProxy)
            val imageWidth = imageProxy.width
            val imageHeight = imageProxy.height

            lastImageWidth = imageWidth
            lastImageHeight = imageHeight

            // Process with TensorFlow Lite
            val results = runInference(bitmap)

            // Update overlay
            results?.let { detections ->
                latestDetections = detections
                activity?.runOnUiThread {
                    fragmentCameraBinding?.overlayView?.setResults(latestDetections, imageWidth, imageHeight)
                    fragmentCameraBinding?.overlayView?.invalidate()
                }
            }

            // Tampilkan rekomendasi jika ada deteksi
            if (!results.isNullOrEmpty()) {
                val bestDetection = results.maxByOrNull { it.confidence }

                bestDetection?.let { detection ->
                    val label = detection.label
                    val info = labelInfoMap[label]

                    if (info != null) {
                        // Update data internal
                        lastLabel = label
                        lastInfo = info.description
                        currentDetection = detection
                        currentDetectionInfo = info.description

                        Log.d(TAG, "Deteksi terbaik: $label dengan confidence: ${detection.confidence}")

                        if (System.currentTimeMillis() - lastUpdateTime > 2000) {
                            lastUpdateTime = System.currentTimeMillis()

                            activity?.runOnUiThread {
                                showDetectionCard(label, info.description)
                            }
                            return@let
                        }
                    } else {
                        Log.d(TAG, "Tidak ada info tersedia untuk label $label")
                    }
                }
            } else {
                Log.d(TAG, "Tidak ada deteksi yang ditemukan")

                // Reset semua state deteksi
                currentDetection = null
                currentDetectionInfo = null
                lastLabel = null
                lastInfo = null

                activity?.runOnUiThread {
                    view?.post {
                        if (::bottomSheetBehavior.isInitialized) {
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        }

                        val rekomendasiTextView = view?.findViewById<TextView>(R.id.rekomendasiTextView)
                        rekomendasiTextView?.text = "Arahkan kamera ke tanaman untuk deteksi..."
                        rekomendasiTextView?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            activity?.runOnUiThread {
                showError("Error memproses gambar: ${e.message}")
            }
        } finally {
            imageProxy.close()
        }
    }

    // Variabel untuk throttling
    private var lastUpdateTime: Long = 0

    @OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        return ImageUtils.imageProxyToBitmap(imageProxy, requireContext())
    }

    private fun runInference(bitmap: Bitmap): List<DetectionResult>? {
        return try {
            Log.d(TAG, "Starting inference with bitmap: ${bitmap.width}x${bitmap.height}")

            // MULAI PENGUKURAN WAKTU
            val startTime = System.currentTimeMillis()

            // Ubah ke TensorImage
            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)
            val processedImage = imageProcessor?.process(tensorImage) ?: return null

            // Siapkan input buffer
            val inputBuffer = TensorBuffer.createFixedSize(
                intArrayOf(1, 640, 640, 3),
                DataType.FLOAT32
            )
            inputBuffer.loadBuffer(processedImage.buffer)

            // Siapkan output array
            val outputArray = Array(1) { Array(25200) { FloatArray(5 + labelList.size) } }

            // Jalankan inference
            interpreter?.run(inputBuffer.buffer, outputArray)

            // HITUNG WAKTU
            val endTime = System.currentTimeMillis()
            val inferenceTime = endTime - startTime
            Log.d(TAG, "Inference completed in ${inferenceTime}ms")

            // Tampilkan waktu ke UI
            activity?.runOnUiThread {
                val inferenceTimeVal = view?.findViewById<TextView>(R.id.inferenceTimeVal)
                inferenceTimeVal?.text = "Inference Time: ${inferenceTime} ms"
            }

            Log.d(TAG, "Output array dimensions: ${outputArray.size} x ${outputArray[0].size} x ${outputArray[0][0].size}")

            // Ubah output ke bentuk float[]
            val flattenedOutput = outputArray[0].flatMap { it.asList() }.toFloatArray()

            // Parse hasil deteksi YOLO
            val detectionResults = parseYoloOutput(flattenedOutput, bitmap.width, bitmap.height)

            Log.d(TAG, "Parsed detections: ${detectionResults.size}")

            return detectionResults

        } catch (e: Exception) {
            Log.e(TAG, "Error running inference", e)
            return null
        }
    }

    private fun parseYoloOutput(
        outputArray: FloatArray,
        imageWidth: Int,
        imageHeight: Int
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResultInternal>()

        val numBoxes = 25200
        val numClasses = labelList.size
        val boxSize = 5 + numClasses

        for (i in 0 until numBoxes) {
            val baseIndex = i * boxSize
            // Parse coordinates dan confidence
            val x = outputArray[baseIndex]
            val y = outputArray[baseIndex + 1]
            val w = outputArray[baseIndex + 2]
            val h = outputArray[baseIndex + 3]
            val objectness = outputArray[baseIndex + 4]

            if (x < 0 || x > 1 || y < 0 || y > 1 || w <= 0 || h <= 0) {
                continue
            }

            var maxClass = 0
            var maxScore = 0f

            for (j in 0 until numClasses) {
                val classScore = outputArray[baseIndex + 5 + j]
                if (classScore > maxScore) {
                    maxScore = classScore
                    maxClass = j
                }
            }

            val finalScore = objectness * maxScore
            val boxArea = w * h

            if (finalScore > 0.3f && boxArea > 0.001f && maxClass < labelList.size) {
                val label = labelList[maxClass]
                results.add(DetectionResultInternal(x, y, w, h, finalScore, maxClass))
                Log.d(TAG, "Detection found: $label, score: $finalScore, area: $boxArea")
            }
        }

        // Terapkan Non-Maximum Suppression untuk hapus overlapping boxes
        val filteredResults = applyNMS(results, iouThreshold = 0.5f)

        // Konversi ke DetectionResult dengan koordinat pixel dan warna label
        return filteredResults.map { detection ->
            DetectionResult(
                label = labelList[detection.classId],
                confidence = detection.score,
                boundingBox = RectF(
                    (detection.x - detection.w / 2) * imageWidth,
                    (detection.y - detection.h / 2) * imageHeight,
                    (detection.x + detection.w / 2) * imageWidth,
                    (detection.y + detection.h / 2) * imageHeight
                ),
                color = getColorForLabel(labelList[detection.classId])
            )
        }
    }

    // Fungsi Non-Maximum Suppression (NMS)
    private fun applyNMS(
        detections: List<DetectionResultInternal>,
        iouThreshold: Float
    ): List<DetectionResultInternal> {
        val sorted = detections.sortedByDescending { it.score }.toMutableList()
        val selected = mutableListOf<DetectionResultInternal>()

        while (sorted.isNotEmpty()) {
            val current = sorted.removeAt(0)
            selected.add(current)

            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                if (iou(current, other) > iouThreshold) {
                    iterator.remove()
                }
            }
        }
        return selected
    }

    // Hitung Intersection over Union (IoU)
    private fun iou(box1: DetectionResultInternal, box2: DetectionResultInternal): Float {
        val box1Left = box1.x - box1.w / 2
        val box1Top = box1.y - box1.h / 2
        val box1Right = box1.x + box1.w / 2
        val box1Bottom = box1.y + box1.h / 2

        val box2Left = box2.x - box2.w / 2
        val box2Top = box2.y - box2.h / 2
        val box2Right = box2.x + box2.w / 2
        val box2Bottom = box2.y + box2.h / 2

        val interLeft = maxOf(box1Left, box2Left)
        val interTop = maxOf(box1Top, box2Top)
        val interRight = minOf(box1Right, box2Right)
        val interBottom = minOf(box1Bottom, box2Bottom)

        val interWidth = (interRight - interLeft).coerceAtLeast(0f)
        val interHeight = (interBottom - interTop).coerceAtLeast(0f)
        val interArea = interWidth * interHeight

        val box1Area = (box1Right - box1Left) * (box1Bottom - box1Top)
        val box2Area = (box2Right - box2Left) * (box2Bottom - box2Top)

        return interArea / (box1Area + box2Area - interArea)
    }

    private fun calculateIoU(a: DetectionResultInternal, b: DetectionResultInternal): Float {
        val aLeft = a.x - a.w / 2
        val aTop = a.y - a.h / 2
        val aRight = a.x + a.w / 2
        val aBottom = a.y + a.h / 2

        val bLeft = b.x - b.w / 2
        val bTop = b.y - b.h / 2
        val bRight = b.x + b.w / 2
        val bBottom = b.y + b.h / 2

        val interLeft = maxOf(aLeft, bLeft)
        val interTop = maxOf(aTop, bTop)
        val interRight = minOf(aRight, bRight)
        val interBottom = minOf(aBottom, bBottom)

        val interWidth = (interRight - interLeft).coerceAtLeast(0f)
        val interHeight = (interBottom - interTop).coerceAtLeast(0f)
        val interArea = interWidth * interHeight

        val areaA = a.w * a.h
        val areaB = b.w * b.h
        val unionArea = areaA + areaB - interArea

        return if (unionArea <= 0f) 0f else interArea / unionArea
    }

    private fun getColorForLabel(label: String): Int {
        return when (label) {
            "Sehat" -> android.graphics.Color.GREEN
            "Kurang_Air" -> android.graphics.Color.BLUE
            "Kalium" -> android.graphics.Color.rgb(255, 165, 0) // orange
            "Kalsium" -> android.graphics.Color.MAGENTA
            "Nitrogen" -> android.graphics.Color.CYAN
            else -> android.graphics.Color.GRAY
        }
    }

    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Deteksi Kamera"
        }
        setHasOptionsMenu(true)

        val dbRef = FirebaseDatabase.getInstance("https://deteksicabai-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("test_connection")
        dbRef.setValue("Hello Firebase!").addOnSuccessListener {
            Log.d("FirebaseTest", "✅ Data tes terkirim ke Firebase!")
        }.addOnFailureListener { e ->
            Log.e("FirebaseTest", "❌ Gagal kirim data tes: ${e.message}")
        }

        // PERBAIKAN: Setup bottom sheet dengan delay yang lebih panjang
        view.postDelayed({
            setupBottomSheetBehavior()

            // Set initial state
            val rekomendasiTextView = view.findViewById<TextView>(R.id.rekomendasiTextView)
            rekomendasiTextView?.text = "Arahkan kamera ke tanaman untuk deteksi..."

            val inferenceTimeVal = view.findViewById<TextView>(R.id.inferenceTimeVal)
            inferenceTimeVal?.text = "Inference Time: 0 ms"

        }, 500) // 500ms delay

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed() // Navigasi balik
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun setupBottomSheetBehavior() {
        val bottomSheetView = fragmentCameraBinding?.bottomSheet
        if (bottomSheetView != null && bottomSheetView.layoutParams is CoordinatorLayout.LayoutParams) {
            try {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)

                // PERBAIKAN: Konfigurasi yang lebih stabil
                bottomSheetBehavior.apply {
                    // Set peek height yang lebih konsisten
                    peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height) // atau 150 jika tidak ada dimen

                    // PENTING: Set state awal ke COLLAPSED
                    state = BottomSheetBehavior.STATE_COLLAPSED

                    // Nonaktifkan dragging sementara untuk stabilitas
                    isDraggable = true
                    isHideable = false

                    // Set fit to contents ke true untuk auto-sizing
                    isFitToContents = true

                    addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                            Log.d("BottomSheet", "State changed to: $newState")
                        }

                        override fun onSlide(bottomSheet: View, slideOffset: Float) {
                            // Handle slide animation jika diperlukan
                        }
                    })
                }

                Log.d("BottomSheet", "Bottom sheet behavior setup complete")

            } catch (e: Exception) {
                Log.e("BottomSheet", "Error setting up bottom sheet behavior", e)
            }
        } else {
            Log.e("BottomSheet", "Bottom sheet view not found")
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = requireContext().assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun showDetectionCard(label: String, info: String) {
        try {
            Log.d(TAG, "showDetectionCard dipanggil dengan label: $label")

            debugSaveState()

            activity?.runOnUiThread {
                val binding = fragmentCameraBinding
                binding?.overlayView?.setResults(
                    latestDetections,
                    lastImageWidth,
                    lastImageHeight
                )
                binding?.overlayView?.invalidate()

                val rekomendasiTextView = fragmentCameraBinding.rekomendasiTextView

                if (rekomendasiTextView != null) {
                    // Format text dengan lebih baik
                    val formattedText = "🔍 HASIL DETEKSI: $label\n\n$info"
                    rekomendasiTextView.text = formattedText

                    // Set text color berdasarkan label
                    val textColor = when (label) {
                        "Sehat" -> ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                        "Kurang_Air" -> ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
                        "Kalium" -> ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
                        "Kalsium" -> ContextCompat.getColor(requireContext(), android.R.color.holo_purple)
                        "Nitrogen" -> ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light)
                        else -> ContextCompat.getColor(requireContext(), android.R.color.black)
                    }
                    rekomendasiTextView.setTextColor(textColor)

                    Log.d(TAG, "Rekomendasi text berhasil di-set: $formattedText")
                } else {
                    Log.e(TAG, "rekomendasiTextView tidak ditemukan")
                }

                // bottom sheet expand dengan delay
                if (::bottomSheetBehavior.isInitialized) {
                    // Gunakan post untuk memastikan UI sudah ready
                    view?.post {
                        try {
                            // Expand bottom sheet untuk menampilkan rekomendasi
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                            Log.d(TAG, "BottomSheet expanded, state: ${bottomSheetBehavior.state}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error expanding bottom sheet", e)
                        }
                    }
                } else {
                    Log.e(TAG, "bottomSheetBehavior belum diinisialisasi")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error showing detection card", e)
        }
    }

    private fun showError(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveDetectionToLocal(label: String, info: String, confidence: Float) {
        try {
            val detection = Detection(
                id = System.currentTimeMillis().toString(),
                label = label,
                info = info,
                confidence = confidence,
                imageUrl = "",
                suggestion = labelInfoMap[label]?.suggestion ?: "Belum ada saran",
                timestamp = System.currentTimeMillis(),
                dateString = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id")).format(Date()),
                imagePath = null // kalau kamu simpan gambar lokal nanti
            )

            Log.d(TAG, "💾 [saveDetectionToLocal] detection=$detection")

            lifecycleScope.launch {
                val dao = DetectionDatabase.getDatabase(requireContext()).detectionDao()
                dao.insert(detection)
                Toast.makeText(requireContext(), "Berhasil disimpan ke lokal!", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ [saveDetectionToLocal] Error: ${e.message}", e)
            Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun debugSaveState() {
        Log.d(TAG, "=== DEBUG SAVE STATE ===")
        Log.d(TAG, "lastLabel: $lastLabel")
        Log.d(TAG, "lastInfo: $lastInfo")
        Log.d(TAG, "currentDetection: $currentDetection")
        Log.d(TAG, "currentDetectionInfo: $currentDetectionInfo")
        Log.d(TAG, "savedDetectionsList size: ${savedDetectionsList.size}")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            interpreter?.close()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get()
            cameraProvider.unbindAll()
        } catch (e: Exception) {
            Log.e("Camera", "Failed to unbind camera", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "✅ onDestroyView: Cleanup resources")

        // Matikan CameraX
        cameraProvider?.unbindAll()

        // Tutup TensorFlow Lite interpreter
        interpreter?.close()

        // Matikan thread executor agar tidak proses frame lagi
        cameraExecutor.shutdown()
    }
}