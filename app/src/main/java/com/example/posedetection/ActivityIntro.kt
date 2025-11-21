package com.example.posedetection

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ActivityIntro : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        val startButton = findViewById<Button>(R.id.startButton)

        startButton.setOnClickListener {
            // Navigate to the main detection page
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}


