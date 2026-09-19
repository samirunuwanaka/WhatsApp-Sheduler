# Agent session summary — StatusFlow

Date: 2026-09-19  
Device: Xiaomi Redmi 9 (`M2004J19C`, Android 12 / API 31) via USB (`2b42ca8e0405`)

## How StatusFlow works

Personal Android app that schedules WhatsApp **messages** or **status** posts. Everything stays on-device (Room DB). There is no Meta/WhatsApp server API — the app wakes the phone, opens WhatsApp with the right intent, and optionally auto-taps Send via Accessibility.

### Core idea: schedule window

Each schedule has:

| Field | Meaning |
|--------|---------|
| **Schedule start** | When the alarm should fire / dispatch may begin |
| **Schedule end** | Last moment catch-up/retry is allowed |
| **Type** | `MESSAGE` (phone + text) or `STATUS` (text) |

Statuses: `PENDING` → `DISPATCHING` → `SENT`, or `FAILED` / `MISSED` / `CANCELLED`.

### Runtime flow

```text
User creates schedule in Compose UI
        │
        ▼
Saved in Room (ScheduleRepository)
        │
        ▼
AlarmScheduler → AlarmManager.setExactAndAllowWhileIdle
        │
        ▼  (at start time, even if screen locked)
ScheduleAlarmReceiver
        │
        ▼
DispatchForegroundService  (notification: "Preparing WhatsApp…")
        │
        ├─ past end?     → MISSED
        ├─ no network?   → FAILED + retry alarm in ~60s (still inside window)
        └─ ok            → DispatchActivity (shows over lock screen)
                                │
                                ▼
                    Arm Accessibility auto-send (SharedPreferences)
                                │
                                ▼
                    Open WhatsApp / Business
                      • MESSAGE → api.whatsapp.com/send?phone=&text=
                      • STATUS  → ACTION_SEND text + jid status@broadcast
                                │
                                ▼
                    AutoSendAccessibilityService clicks Send/Post
                                │
                                ▼
                              SENT
```

### Boot / power-off catch-up

`BootReceiver` on `BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED` / app update:

- **Before start** → re-arm the exact alarm  
- **Inside start…end** → dispatch immediately  
- **After end** → mark `MISSED`

### Permissions that matter

- Exact alarms (reliable fire while idle/locked)
- Ignore battery optimizations (OEM killers)
- Accessibility → StatusFlow (auto-tap Send; optional but recommended)
- Notifications / foreground service (dispatch while background)

### Limits (by design)

- WhatsApp must be installed and logged in; delivery still needs mobile data/Wi‑Fi
- Auto-send is UI automation, not a silent API — WhatsApp UI changes can break the tap
- Use only on accounts you own

### Project map

```text
app/src/main/java/com/statusflow/scheduler/
  data/          Room entities, DAO, repository
  scheduler/     AlarmManager, boot catch-up, foreground dispatch
  whatsapp/      Intents + Accessibility auto-send
  ui/            Compose screens, DispatchActivity, theme
```

---

## Problem

Installing the release APK on the phone failed with:

> App not installed as package appears to be invalid

Root cause: `assembleRelease` produced an **unsigned** APK (`app-release-unsigned.apk`). Modern Android rejects unsigned packages.

## What was done

### Build / signing

- Generated a release keystore: `app/statusflow-release.jks` (gitignored)
- Added `keystore.properties` (gitignored) with store/key credentials
- Updated `app/build.gradle.kts` so the **release** build type uses that signing config (falls back to debug signing if properties are missing)
- Updated `.gitignore` to ignore `*.jks`, `*.keystore`, and `keystore.properties`
- Built successfully:
  - Debug: `app/build/outputs/apk/debug/app-debug.apk`
  - Signed release: `app/build/outputs/apk/release/app-release.apk` (APK Signature Scheme v2 verified)

### Docs

- Updated `README.md`: signed APK path, warning about unsigned packages, Xiaomi **Install via USB** steps, and `adb` install commands using `D:\Android\Sdk\platform-tools\adb.exe`

### Git

- Commit: `446538f` — *Fix release APK signing so installs are valid on device.*
- Tag: `v1.0.0`
- Pushed to `origin/main` and pushed tag `v1.0.0`  
  Remote: https://github.com/samirunuwanaka/WhatsApp-Sheduler.git

### Device collaboration (USB)

- Confirmed `adb` connectivity to the Redmi 9
- Verified APK signatures with `apksigner`
- Copied APK to phone: `/sdcard/Download/StatusFlow.apk`
- Also staged under `/data/local/tmp/StatusFlow.apk`
- Opened Developer options on the device via intent
- **Install still blocked** by MIUI:
  - `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`
  - Needs **Developer options → Install via USB** (and often **USB debugging (Security settings)**) enabled, usually with a Mi Account
- Screen interaction was further limited by MIUI pocket/proximity lock (`ScreenOnProximitySensorGuide`) and keyguard, so UI automation could not finish install or run performance checks

## Not completed (blocked on device)

- `adb install` of StatusFlow
- Launch / cold-start / memory / logcat performance validation on-device

## Next step for the user

1. Unlock the phone and clear proximity/pocket cover if shown  
2. Enable **Install via USB** (+ security USB debugging if listed)  
3. Ask the agent to **retry** — then install, launch, and measure real performance  

Or install manually: **Files → Download → StatusFlow.apk**
