# Hand-Raise-Detection
This Android application detects when a user raises their hand using the device camera and provides a voice response using Text-to-Speech (TTS).


## 📱 What does it do?
The app opens the camera and continuously scans for body language. When it sees you raising your hand Supports both hands(left and right) specifically, when your wrist goes above your shoulder, it triggers a Text-to-Speech engine to say, *"Hand detected, how can I help you?"*

I focused on making the detection "stable" rather than just sensitive, so it doesn't trigger accidentally when you are just moving around.

## 🛠️ Tech Stack & Libraries
* **Language:** Kotlin
* **Architecture:** MVVM (Model-View-ViewModel)
* **ML Library:** Google ML Kit (Pose Detection - Accurate Model)
* **Camera:** CameraX
* **UI:** XML with Material Design components

## 🧠 How the Logic Works
Detecting a hand raise sounds simple, but there are a few edge cases I had to handle. Here is the logic I implemented:

1.  **Coordinate Check:** In Android, the Y-axis value *decreases* as you go up the screen. So, I check if the `Wrist_Y` coordinate is significantly smaller than the `Shoulder_Y` coordinate.
2.  **The "Buffer" Zone:** To prevent false positives (like when your hand is just resting near your shoulder), I added a 5% threshold buffer. The wrist has to be clearly above the shoulder line.
3.  **Debouncing (Stability):** Cameras can be noisy. I implemented a **5-frame persistence check**. The AI must agree that the hand is raised for 5 consecutive video frames before it "counts."
4.  **Voice Cooldown:** To stop the app from spamming the voice message repeatedly, I added a 10-second timer. Once it speaks, it won't speak again for 10 seconds, even if you keep your hand up.

## 🏗️ Architecture (MVVM)
I followed the MVVM pattern to keep the code clean:
* **`MainActivity` (View):** Handles the Camera permission, setups up the UI, and manages the Text-to-Speech engine. It observes changes but doesn't do any math.
* **`PoseViewModel` (ViewModel):** This is the brain. It receives raw data from ML Kit, performs the coordinate math and frame counting, and decides when to update the UI or trigger the voice.
* **`DetectionResult` (Model):** A simple data class that holds the current state.

## 📸 Screenshots
<p align="center">
<img width="250" height="440" alt="image" src="https://github.com/user-attachments/assets/3bb3d00d-22e5-4aaf-a2b8-41e75fc2f84f" />
<img width="250" height="440" alt="image" src="https://github.com/user-attachments/assets/57b74728-37cf-476c-a75c-6ac3a740916a" />
</p>

## 🚀 How to Run
1.  Clone the repository.
2.  Open in Android Studio.
3.  Run on a physical device (recommended) or an emulator with camera support.
4.  Accept the Camera permission when prompted.

---
*Submitted by Harish V*
