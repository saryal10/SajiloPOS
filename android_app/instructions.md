# How to run the SajiloPOS Android app

This guide takes you from zero to the app running on an emulator or a real
phone, plus how to build, test, and troubleshoot. All commands run from this
folder (`android_app/`).

---

## 1. What you need

| Requirement | Details |
|---|---|
| Computer | macOS, Windows, or Linux with at least 8 GB RAM (16 GB recommended) and ~10 GB free disk |
| Java (JDK) | **JDK 17 or newer** (JDK 21 recommended). Android Studio bundles one, so you usually don't install this separately |
| Android Studio | Latest stable from <https://developer.android.com/studio> (includes the Android SDK) |
| Android SDK | API level 36 (Android 16) installed via SDK Manager. The app also runs on any device/emulator with **Android 7.0 (API 24)** or newer |

> The project ships its own Gradle wrapper (`gradlew`, `gradle/wrapper/gradle-wrapper.jar`,
> Gradle 9.3.1), so you do **not** need to install Gradle yourself.

---

## 2. Get the code

```bash
git clone <your-repo-url>
cd SajiloPOS/android_app
```

---

## 3. Open the project in Android Studio

1. Open Android Studio → **Open** → select the `android_app` folder (the one containing `settings.gradle.kts`).
2. Wait for **Gradle sync** to finish (watch the progress bar at the bottom). First sync downloads dependencies and takes a few minutes.
3. If Android Studio asks to install a missing SDK platform, accept it.

> On first open, Android Studio creates a `local.properties` file pointing at
> your SDK location. That file is machine-specific and is **not** committed.

---

## 4a. Run on an emulator (no phone needed)

1. In Android Studio, open **Device Manager** (phone icon in the right toolbar, or *Tools → Device Manager*).
2. **Create device** → choose **Pixel 7** (or any Pixel) → **Next**.
3. Pick a system image with **API 36** (or anything API 24+; Google Play images are fine) → **Next** → **Finish**.
4. Press the ▶ **Run** button in the top toolbar (make sure the `app` configuration and your new emulator are selected).
5. The emulator boots, installs the app, and launches it automatically.

Tips:
- The barcode scanner uses the camera. On the emulator, open the emulator's
  extended controls (**⋯**) → **Camera** and set both cameras to **VirtualScene**,
  or simply use the **demo barcodes / manual entry** inside the scanner — no camera needed.
- If the emulator is slow, choose **Cold Boot Now** from the Device Manager ⋮ menu to reset it.

---

## 4b. Run on a real Android phone

1. On the phone: **Settings → About phone → tap “Build number” 7 times** to enable Developer options.
2. Go to **Developer options → enable USB debugging** (and on some phones, “USB debugging (Security settings)”).
3. Connect the phone to your computer with a USB cable. Accept the **“Allow USB debugging?”** prompt on the phone.
4. In Android Studio's device dropdown (top toolbar), select your phone instead of the emulator.
5. Press ▶ **Run**.
6. **Windows only:** if the phone isn't detected, install the [Google USB driver](https://developer.android.com/studio/run/win-usb).

---

## 5. Build & install from the command line

You don't need Android Studio open for these. Run from the `android_app/` folder:

```bash
# macOS / Linux
./gradlew assembleDebug      # builds app/build/outputs/apk/debug/app-debug.apk

# Windows (Command Prompt / PowerShell)
gradlew.bat assembleDebug
```

Install a built APK onto a connected emulator or phone:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Launch it directly (handy after reinstalling):

```bash
adb shell am start -n com.aistudio.sajilopos.npqzr/com.example.MainActivity
```

Find connected devices:

```bash
adb devices
```

(`adb` lives in your SDK's `platform-tools` folder, e.g. `~/Library/Android/sdk/platform-tools` on macOS or `%LOCALAPPDATA%\Android\Sdk\platform-tools` on Windows.)

---

## 6. Run the tests

```bash
./gradlew testDebugUnitTest   # 73 fast JVM unit tests across 8 files
```

HTML reports land in `app/build/test-results/testDebugUnitTest/`. Device/instrumented tests (if any) run with:

```bash
./gradlew connectedDebugAndroidTest   # needs an emulator or phone attached
```

---

## 7. Take it for a spin (5-minute test script)

1. **Sell tab** — tap the big **SCAN** hero, pick a demo barcode (or type one manually). The item lands in the cart.
2. **Checkout** — tap the cart bar → **Pay**. Try **Cash** (tendered chips auto-fill, change is computed) and a wallet method (dynamic QR + simulated gateway verification).
3. **Receipt** — after payment, preview the 58/80 mm thermal receipt, try Print / Share / copy ESC/POS.
4. **Stock tab** — tap **+**, then the **scan icon** inside *Barcode / SKU* to fill a code from the camera; enter quantity, cost and selling price, save.
5. **Insights tab** — revenue, payment mix, top sellers and restock suggestions update from the sales you just made (works fully offline).
6. **History tab** — find the sale, tap to reprint the receipt.
7. **Settings tab** — shop name, PAN/VAT, VAT on/off, paper width. (The app is retail-only; there is no business-type switcher.)

To start over with a clean database: on the emulator/phone, go to
**Settings → Apps → SajiloPOS → Storage → Clear data** (or uninstall and reinstall).

---

## 8. Troubleshooting

| Symptom | Fix |
|---|---|
| Gradle sync fails / “SDK not found” | Open SDK Manager, install **Android SDK Platform 36** + Build-Tools; check `local.properties` points at your SDK |
| “Unsupported Java version” / toolchain errors | Set Android Studio to an embedded or installed **JDK 17+** (*Settings → Build Tools → Gradle → Gradle JDK*) |
| Emulator won't boot / very slow | Enable virtualization (VT-x / Hyper-V / HVF), allocate 4 GB RAM to the AVD, or use a real phone instead |
| Camera shows black in scanner | Set emulator cameras to **VirtualScene**, or use demo barcodes / manual entry |
| Printer not found | Expected without hardware — use **Share** or **copy ESC/POS** on the receipt screen instead |
| Payment “verification” hangs | It's a simulated gateway; wait a few seconds for the `205_VERIFIED_OK` step, on a real device or emulator alike |
| App data looks stale after code changes | Uninstall the app from the device (`adb uninstall com.aistudio.sajilopos.npqzr`), then reinstall |

---

## 9. Cheat sheet

```bash
./gradlew assembleDebug        # build debug APK
./gradlew installDebug         # build + install on the attached device
./gradlew testDebugUnitTest    # run unit tests (73 tests)
adb devices                     # list emulators/phones
adb install -r <apk>            # install/reinstall an APK
adb uninstall com.aistudio.sajilopos.npqzr   # remove app + wipe its database
```

Questions or problems? Open an issue in the repo with your OS, Android Studio
version, and the exact error text.
