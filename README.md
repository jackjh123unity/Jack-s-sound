# StoryCast Audio - AI Audiobook Studio & Ebook Reader

StoryCast Audio transforms Word documents, Google Docs, text files, and eBooks into theatrical, dynamic audiobooks powered by **Google AI Studio Gemini 3.8 Flash TTS** (`gemini-3.8-flash-tts`), custom voice tone synthesis, and local offline fallback engines.

---

## 📄 Google Docs, Word (.docx), & Document Support

StoryCast Audio now fully supports:
1. **Microsoft Word (`.docx`, `.doc`)**: Clean native OpenXML paragraph and dialog extraction with accurate page breaks.
2. **Google Docs**: 
   - Open your Google Doc, tap **File → Download → Microsoft Word (.docx)** (or Plain Text `.txt`) and select it in StoryCast Audio.
   - Or tap **"Paste Google Doc Text / Link"** directly in the app to paste your story text or share URL!
3. **Plain Text (`.txt`, `.md`)**: Instant chapter pagination for scripts and plain text stories.
4. **Clean PDF Fallback**: Filters out raw internal stream syntax and warns if a PDF is a scanned image, recommending Word or Google Doc export for optimal voice acting.

---

## 🎧 Dual Audio Download Options (Single Chapter & Full Single-File)

StoryCast Audio provides two dedicated audio export buttons in both the **Reader Screen** and the **Audio Player**:

1. **📦 All Chapters (1-File)**:
   - Synthesizes every chapter in sequence with authentic narrator inflections.
   - Stitches all chapters into one unified, uninterrupted continuous audiobook file (`.wav`).
   - Automatically saves to your device's `Music/StoryCastAudio/` folder for offline listening in any music app.

2. **⬇ Download Chapter X**:
   - Synthesizes and downloads **1 chapter at a time**.
   - Perfect for quick chapter-by-chapter listening without waiting for the entire book.
   - Shows live synthesis progress and exports directly to device audio storage.

3. **📲 Offline & Sharing**:
   - Both download types feature immediate **"Play Audio"** inside StoryCast Audio and **"Share / Save"** via the Android share sheet (Google Drive, Files, WhatsApp, etc.).

---

## 📱 How to Install the App on Your Android Phone

### 🌟 Where is the APK File?
- **Inside the Downloaded Files**: If you downloaded or exported the project as a ZIP, extract it on your phone. Right inside the folder, you will find:
  👉 **`StoryCast-Audio.apk`** (also in the `app-apk/` folder)
  Simply tap this file in your phone's file manager to install!

- **Direct Download from GitHub Releases (Fastest - No Unzipping Needed)**:
  1. Open this repository in your phone's browser (Chrome, Samsung Internet, Firefox, etc.).
  2. Tap **Releases** (or look for **"Latest Android APK Build"**).
  3. Under **Assets**, tap **`StoryCast-Audio.apk`**.
  4. Once downloaded, tap the file in your notification bar or Downloads folder to install!

---

### 📲 Step-by-Step Android Installation Guide:

1. **Locate the `.apk` File**:
   - If you downloaded the ZIP: Open your phone's **Files** or **Downloads** app, tap **Extract**, and tap **`StoryCast-Audio.apk`**.
   - If you downloaded from Releases: Tap the download notification or tap `StoryCast-Audio.apk` in your Downloads.

2. **Allow Installation from Unknown Sources (First time only)**:
   - Android may show a prompt: *"For your security, your phone is not allowed to install unknown apps from this source"*.
   - Tap **Settings**.
   - Toggle ON **"Allow from this source"** (for Chrome, Files, or whichever browser/file manager you used).
   - Press the **Back** button.

3. **Install**:
   - Tap **Install**.
   - Once installation finishes, tap **Open** to enjoy **StoryCast Audio**!

---

### 📦 Alternative: Download from GitHub Actions Tab
1. Open the repository on GitHub and tap the **Actions** tab.
2. Tap the latest workflow run (e.g., **"Build & Release Android APK"**).
3. Scroll down to the **Artifacts** section at the bottom.
4. Tap **`StoryCast-Audio-APK`** to download. Extract the zip on your phone to find `StoryCast-Audio.apk`.

---

### 💻 Local Build (from Terminal or Android Studio)
To build the APK locally on your machine:
```bash
./gradlew assembleDebug
```
The generated APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk` or in `app-apk/StoryCast-Audio.apk`.

---

## 🎙️ Voice Values & Fine-Tuning (Pitch, Speed, Timbre)

You can customize narrator voices to match any character or preference:

1. **Voice Pitch / Timbre (0.60x to 1.60x)**:
   - **Deep Baritone (0.70x)**: Deep, resonant, masculine timbre for arch-narrators, warlords, and ancients.
   - **Warm Bass (0.85x)**: Grounded, authentic male voice.
   - **Natural Studio (1.00x)**: Unaltered baseline pitch from the studio model.
   - **Crisp & Clear (1.20x)**: Higher melodic clarity.
   - **Bright / High (1.40x)**: High vocal register for younger protagonists and spirited dialogue.

2. **Reading Speed / Rate (0.50x to 2.50x)**:
   - Presets for **0.75x**, **1.00x**, **1.25x**, **1.50x**, and **2.00x**.
   - Smooth continuous slider for granular control.

3. **Live Voice Audition**:
   - In the **Voice Roster Sheet**, open the **"Fine-Tune Voice Values"** panel.
   - Adjust the pitch and speed sliders, then click **"Test Voice Tuned"** or tap **"Audition"** on any of the 10 character cards to hear that voice rendered with your custom pitch and speed in real time!

4. **Real-Time In-Player Adjustment**:
   - Tap the **Speed Pill** or **Pitch Pill** on the player screen to adjust pitch and speed during playback. Changes take effect immediately!

---

## 🌟 Key Features

- **Gemini 3.8 Flash TTS Engine**: Multi-tonal, humanized speech synthesis with vocal breathing, emotional pacing, and authentic masculine and feminine voices.
- **10 Curated Character Voices**: 5 distinct Male voices (Arthur, Marcus, Jonathan, Julian, Rowan) and 5 distinct Female voices (Eleanor, Lyra, Beatrice, Morgana, Maeve).
- **1-Click Single-File Continuous Audiobook Export**: Synthesizes and stitches all pages into a unified `.wav` audiobook file for offline listening.
- **Audio Spectrum Visualizer**: Dynamic visualizer that bounces and illuminates during playback.
- **Full Offline Fallback**: Works offline with local Android TTS engine configured with strict masculine/feminine pitch constraints.
