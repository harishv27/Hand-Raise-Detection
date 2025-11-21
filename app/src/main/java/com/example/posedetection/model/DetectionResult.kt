package com.example.posedetection.model

data class DetectionResult(
    val isHandRaised: Boolean = false,
    val statusMessage: String = "Searching..."
)