#PeerPitchKotlinVer

PeerPitch is an Android presentation feedback app built with Kotlin and Jetpack Compose. It helps users improve their public-speaking skills by analyzing eye contact, filler words, and speech quality during practice presentations.

✨ Features

👁️ Eye Contact Tracking — Uses MediaPipe Face Landmarker to analyze facial landmarks and estimate eye contact of the user in real time.

🗣️ Filler Word Detection — Uses offline speech-to-text to identify filler words such as "um," "uh," and other bad verbal habits 

🤖 AI-Powered Feedback — Google Gemini evaluates speech quality and provides personalized coaching feedback.

📊 Presentation Score — Generates a deterministic score based on eye contact, filler-word rate, and AI-evaluated speech quality.

🔒 Offline Speech Recognition — Vosk provides speech-to-text processing directly on the device.

🔐 Firebase Authentication — Supports user authentication through Firebase Auth.

🛠️ Tech Stack
Technology	Purpose
Kotlin	Android application development
Jetpack Compose	Modern declarative UI
CameraX	Camera and live video processing
MediaPipe Face Landmarker	Facial landmark and eye-contact tracking
Vosk	Offline speech-to-text
Google Gemini	AI-powered speech evaluation and coaching
Firebase Auth	User authentication
🧠 How It Works

PeerPitch combines multiple signals from a presentation session:

Camera ──► MediaPipe Face Landmarker ──► Eye Contact Score
   │
   └─────────────────────────────────────────────┐
                                                 │
Microphone ──► AudioRecord ──► Vosk ──► Filler Words
                                                 │
                                                 ▼
                                      Presentation Scoring
                                                 │
                          ┌──────────────────────┴──────────────────────┐
                          ▼                                             ▼
                   Deterministic Score                         Gemini Speech Analysis
                          │                                             │
                          └──────────────────┬──────────────────────────┘
                                             ▼
                                  Final 0–100 Score
