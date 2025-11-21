package com.example.posedetection.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.posedetection.model.DetectionResult
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class PoseViewModel : ViewModel() {

    private val _detectionState = MutableLiveData<DetectionResult>()
    val detectionState: LiveData<DetectionResult> = _detectionState

    private val _ttsTrigger = MutableLiveData<String>()
    val ttsTrigger: LiveData<String> = _ttsTrigger

    // --- SETTINGS ---
    private val SPEECH_COOLDOWN_MS = 10_000L // 10 Seconds between voice messages
    private val REQUIRED_FRAME_COUNT = 5     // Hand must be up for 5 frames to count

    // --- STATE VARIABLES ---
    private var lastSpeechTime: Long = 0
    private var positiveFrameCount = 0
    private var isCurrentlyRaised = false

    init {
        _detectionState.value = DetectionResult()
    }

    fun processPose(pose: Pose) {
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        // Buffer: Wrist must be 5% higher than shoulder to be sure
        val thresholdBuffer = 0.05f

        var isDetectedNow = false
        var message = "Searching..."

        // 1. Check Left Hand (Y increases downwards, so LOWER Y means HIGHER on screen)
        if (leftWrist != null && leftShoulder != null) {
            if (leftWrist.position.y < (leftShoulder.position.y * (1 - thresholdBuffer))) {
                isDetectedNow = true
                message = "Left Hand Detected"
            }
        }

        // 2. Check Right Hand (if left not found)
        if (!isDetectedNow && rightWrist != null && rightShoulder != null) {
            if (rightWrist.position.y < (rightShoulder.position.y * (1 - thresholdBuffer))) {
                isDetectedNow = true
                message = "Right Hand Detected"
            }
        }

        // 3. Persistence Check (Filter out quick glitches)
        if (isDetectedNow) {
            positiveFrameCount++
        } else {
            positiveFrameCount = 0
            isCurrentlyRaised = false // Reset lock when hand goes down
        }

        val isConfirmedRaise = positiveFrameCount >= REQUIRED_FRAME_COUNT

        // 4. Trigger Voice (Only if confirmed AND cooldown passed)
        if (isConfirmedRaise && !isCurrentlyRaised) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSpeechTime > SPEECH_COOLDOWN_MS) {
                _ttsTrigger.postValue("Hand detected, how can I help you?")
                lastSpeechTime = currentTime
                isCurrentlyRaised = true // Lock until hand goes down
            }
        }

        // 5. Update UI Text
        _detectionState.postValue(DetectionResult(
            isHandRaised = isConfirmedRaise,
            statusMessage = if (isConfirmedRaise) "$message (Confirmed)" else "Searching..."
        ))
    }
}