package com.example.posedetection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.posedetection.databinding.ActivityMainBinding
import com.example.posedetection.viewmodel.PoseViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding // Reference to the generated binding class
    private val viewModel: PoseViewModel by viewModels()
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var poseDetector: PoseDetector
    private lateinit var textToSpeech: TextToSpeech

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            binding.statusTextView.text = "Camera permission denied."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- View Binding Initialization ---
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // Set the root view from the binding object
        // -----------------------------------

        cameraExecutor = Executors.newSingleThreadExecutor()

        // 1. Initialize ML Kit Pose Detector
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        poseDetector = PoseDetection.getClient(options)

        // 2. Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this, this)

        // 3. Setup Observers (MVVM)
        setupObservers()

        // 4. Request Camera Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // --- MVVM Observers ---

    private fun setupObservers() {
        // Observe the overall detection state
        viewModel.detectionState.observe(this) { result ->
            binding.statusTextView.text = result.statusMessage
            // Optional: Draw landmarks here (Bonus Feature)
        }

        // Observe the TTS trigger
        viewModel.ttsTrigger.observe(this) { message ->
            speak(message)
        }
    }

    // --- Text-to-Speech Implementation ---

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    // --- CameraX and ML Kit Integration ---

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) } // Use binding to access previewView

            // Image Analysis for ML Kit
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, PoseDetectionAnalyzer(poseDetector, viewModel::processPose))
                }

            // Select back camera, though front camera is often better for pose detection
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    // --- ML Kit Analyzer Class ---

    private class PoseDetectionAnalyzer(
        private val detector: PoseDetector,
        private val listener: (com.google.mlkit.vision.pose.Pose) -> Unit
    ) : ImageAnalysis.Analyzer {

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                detector.process(image)
                    .addOnSuccessListener { pose ->
                        // Pass the Pose object to the ViewModel for processing
                        listener(pose)
                    }
                    .addOnFailureListener { e ->
                        Log.e("MLKit", "Pose detection failed: $e")
                    }
                    .addOnCompleteListener {
                        // Crucial: close the imageProxy when done
                        imageProxy.close()
                    }
            }
        }
    }
}