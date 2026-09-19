# StatusFlow

Personal WhatsApp **message** and **status** scheduler for Android. Schedules are stored on the phone, alarms fire while the screen is locked, and if the phone was powered off, StatusFlow catches up **as long as you are still inside the schedule window** (schedule start → schedule end).

> WhatsApp itself needs mobile data or Wi‑Fi to *deliver* a message. StatusFlow keeps working offline for planning and retries inside your selected period once the network returns.

## Features

- Schedule **Message** (phone + text) or **Status** (text)
- **Schedule start** and **Schedule end** window
- Exact alarms that can wake the device while locked
- Boot catch-up after the phone was turned off
- Offline-aware retry until the end of the window
- Accessibility auto-tap of WhatsApp **Send** (optional but recommended)
- Forest-night Material UI (Compose)

## Build the APK (on your PC)

### Requirements

- JDK 17+ (Java 21 works)
- Android SDK platform 34 + build-tools
- This project (Gradle wrapper included)

### Commands (Windows PowerShell)

```powershell
cd "D:\Ideas\WhatsApp Status message sheduler"
.\gradlew.bat assembleRelease
```

Release APK path:

```text
app\build\outputs\apk\release\app-release-unsigned.apk
```

Debug APK (easier for USB install during development):

```powershell
.\gradlew.bat assembleDebug
```

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Install on a phone with USB only (no Play Store / no third-party APK sites)

You only need: a USB cable, the phone, and the official **Android SDK Platform-Tools** `adb` (or Android Studio). No APK mirror apps required.

### 1. On the phone

1. Open **Settings → About phone**.
2. Tap **Build number** seven times to unlock developer options.
3. Open **Settings → System → Developer options**.
4. Enable **USB debugging**.
5. (Optional but useful) Enable **Install via USB** if your OEM shows it.
6. Unlock the phone screen and keep it unlocked while installing.

### 2. On the PC

1. Install [Android Platform-Tools](https://developer.android.com/tools/releases/platform-tools) from Google (official ZIP).
2. Unzip it somewhere simple, e.g. `C:\platform-tools`.
3. Connect the phone with USB. Choose **File transfer / MTP** if asked.
4. Accept the **Allow USB debugging?** prompt on the phone.

### 3. Install the APK

In PowerShell:

```powershell
cd C:\platform-tools
.\adb.exe devices
.\adb.exe install -r "D:\Ideas\WhatsApp Status message sheduler\app\build\outputs\apk\debug\app-debug.apk"
```

- `adb devices` must list your device as `device` (not `unauthorized`).
- `-r` replaces an older install if present.

If install is blocked by the phone, open the notification / prompt and allow installation from that USB session, then run the `adb install` command again.

### 4. First-run setup in StatusFlow

1. Open **StatusFlow**.
2. Tap the gear icon and enable:
   - **Exact alarms**
   - **Battery unrestricted** (ignore battery optimizations)
   - **Accessibility → StatusFlow** (auto-tap Send)
3. Install **WhatsApp** or **WhatsApp Business** and stay logged in.
4. Create a schedule with **Schedule start** and **Schedule end**.

## How the schedule window works

| Situation | Behavior |
|-----------|----------|
| Phone on & locked at start time | Alarm wakes StatusFlow and opens WhatsApp |
| Phone off during start, powered on before end | Boot receiver dispatches catch-up |
| No internet at fire time | Marked for retry; tries again until **schedule end** |
| Current time past **schedule end** | Marked **Missed** — will not send |

## Privacy & limits

- Schedules stay in a local Room database on your device.
- There is no official consumer WhatsApp API for silent background send; StatusFlow opens WhatsApp and (with Accessibility enabled) taps Send for you.
- Use only on accounts you own. Do not use this for spam or harassment.

## Project layout

```text
app/src/main/java/com/statusflow/scheduler/
  data/          Room entities & repository
  scheduler/     AlarmManager, boot catch-up, foreground dispatch
  whatsapp/      Intents + Accessibility auto-send
  ui/            Compose screens & theme
```

## License

Personal / educational use. WhatsApp is a trademark of Meta Platforms, Inc. This project is not affiliated with Meta.
