<div align="center">

<img src="https://iili.io/nja6u0N.jpg">

# 🛸 Sub-Etha OmniView

### *Don't panic. Watch up to four videos at once.*

Sub-Etha OmniView is a multi-video splitscreen player for Android phones, tablets, Android TV and Fire TV that plays up to four videos simultaneously in nine customizable layouts, with independent playback, volume and speed controls, video rotation, zoom and fit/fill options, video or folder looping, tile swapping, a built-in file browser and full TV remote support — built on **Media3 / ExoPlayer**.

![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-ExoPlayer%201.4.1-00E5FF)
![TV](https://img.shields.io/badge/Android%20TV%20%2F%20Fire%20TV-D--pad%20ready-00838F)

*Created by [burnsymbol.com](https://burnsymbol.com)*

</div>

---

## 📖 Table of Contents

1. [✨ Highlights](#-highlights)
2. [🚀 Quick Start](#-quick-start)
3. [🧭 The Splash Screen](#-the-splash-screen)
4. [🧱 Layouts](#-layouts)
5. [🎬 The Player Screen](#-the-player-screen)
6. [🎛️ Per-Video Controls](#️-per-video-controls)
7. [🔁 Loop Modes](#-loop-modes)
8. [🔀 Swapping Videos](#-swapping-videos)
9. [📁 The Built-in Video Browser](#-the-built-in-video-browser)
10. [📺 Android TV & Fire TV (Remote Control)](#-android-tv--fire-tv-remote-control)
11. [⚙️ Performance & Decoders](#️-performance--decoders)
12. [🔐 Permissions](#-permissions)
13. [🛠️ Building the App](#️-building-the-app)
14. [🗂️ Project Structure](#️-project-structure)
15. [🩺 Troubleshooting & Tips](#-troubleshooting--tips)

---

## ✨ Highlights

| | Feature |
|---|---|
| 🖼️ | **1 to 4 videos at once**, each with its **own independent player** |
| 🧱 | **9 screen layouts** — halves, big-and-small, columns, 2×2 grid |
| 🔊 | **Per-video volume slider** (0–100%) with live percentage |
| ⏩ | **Per-video playback speed** from 0.5× to 2× |
| 🔄 | **Per-video rotation** in 90° steps |
| 🔍 | **Fill / Fit button** to remove the black letterbox bars |
| 🤏 | **Pinch-to-zoom, pan and double-tap reset** on every video |
| 🔁 | **Three loop modes** — this video, the **whole folder**, or off |
| 🔀 | **Swap button** to move videos around the grid |
| 📁 | **Built-in file explorer** with thumbnails, durations and details |
| 📺 | **Full D-pad / remote support** for Android TV and Fire TV |
| 🫥 | **Auto-hiding controls** — nothing blocks your picture |
| ⚙️ | **Smart decoder handling** so TVs can play all four videos |
| 🌌 | **Dark + cyan design** with a Hitchhiker's-Guide-style icon |

---

## 🚀 Quick Start

1. 📲 **Install** the app (see [Building the App](#️-building-the-app)).
2. 🔢 Choose **how many videos** you want: **1, 2, 3 or 4**.
3. 🧱 Pick a **layout** (a live preview shows exactly what you'll get).
4. 📂 Tap **SELECT** on each video slot and choose a file.
5. ▶️ Press **START PLAYING**.

> 💡 **Just want to watch one video?** Tap **BROWSE VIDEOS** at the top of the splash screen, open a folder, and tap any video.

---

## 🧭 The Splash Screen

The first screen you see. Everything is built to work with **touch** *and* a **TV remote**.

### 🎛️ What's on it

| Element | What it does |
|---|---|
| 🔗 **Credit link** (top right) | Opens *burnsymbol.com* in your browser |
| 📂 **BROWSE VIDEOS** | Opens the built-in explorer to play **a single video** |
| 🔢 **Number of videos** | Chips for **1 / 2 / 3 / 4** |
| 🖼️ **Layout preview** | A live diagram of the layout you've chosen, with video numbers |
| 🧩 **Layout chips** | Appear when there's more than one layout for your video count |
| 📋 **Video slots** | One card per video — shows the chosen **file name** and a **SELECT / CHANGE** button |
| ▶️ **START PLAYING** | Greyed out until every slot has a video, and counts down: *"SELECT 2 MORE VIDEOS"* |

### 🧠 Nice little touches

- ♻️ The app **remembers your layout choice for each video count** — switch from 3 to 2 and back, and your 3-video layout is still selected.
- ✅ Your **picked videos stay put** when you change the count or layout.
- 🎯 On a TV remote, **focus returns to where you were** after a list refreshes or a file is picked.
- 🔙 Pressing **Back** on the player brings you back here with everything intact.

---

## 🧱 Layouts

Slot **1** is always the **big** video wherever there is one.

### 1️⃣ One video

Full screen.

### 2️⃣ Two videos

**Vertical split** — side by side &nbsp;&nbsp;&nbsp;&nbsp; **Horizontal split** — stacked

```
┌─────────┬─────────┐        ┌───────────────────┐
│         │         │        │         1         │
│    1    │    2    │        ├───────────────────┤
│         │         │        │         2         │
└─────────┴─────────┘        └───────────────────┘
```

### 3️⃣ Three videos

**Big left** (50% + two 25%) &nbsp;&nbsp; **Big top** (50% + two 25%)

```
┌─────────┬─────────┐        ┌───────────────────┐
│         │    2    │        │         1         │
│    1    ├─────────┤        ├─────────┬─────────┤
│         │    3    │        │    2    │    3    │
└─────────┴─────────┘        └─────────┴─────────┘
```

**Big right** (50% on the right, two 25% on the left) &nbsp;&nbsp; **3 vertical** (three columns)

```
┌─────────┬─────────┐        ┌───────┬───────┬───────┐
│    2    │         │        │       │       │       │
├─────────┤    1    │        │   1   │   2   │   3   │
│    3    │         │        │       │       │       │
└─────────┴─────────┘        └───────┴───────┴───────┘
```

### 4️⃣ Four videos

**2 × 2 grid** &nbsp;&nbsp;&nbsp;&nbsp; **4 vertical** (four columns)

```
┌─────────┬─────────┐        ┌─────┬─────┬─────┬─────┐
│    1    │    2    │        │     │     │     │     │
├─────────┼─────────┤        │  1  │  2  │  3  │  4  │
│    3    │    4    │        │     │     │     │     │
└─────────┴─────────┘        └─────┴─────┴─────┴─────┘
```

> 🖤 **No borders, no gaps.** Tiles sit perfectly flush against each other so your video gets every pixel.
>
> 📱 The **vertical-column** layouts are perfect for **portrait (phone-shot) videos**, or for a phone held sideways.

---

## 🎬 The Player Screen

- 🖥️ **Immersive fullscreen** — system bars are hidden (swipe from the edge to peek at them).
- 💡 **Screen stays on** while you watch.
- ▶️ Every video has **its own ExoPlayer** — play, pause, seek and tweak each one independently.
- 🔁 Videos **loop forever** by default.
- ⏸️ Leave the app and come back: each video **resumes exactly where it was**, with all your settings intact.
- 🔄 Rotating your device **doesn't interrupt playback**.
- 🔙 **Back** returns to the splash screen (or cancels a swap in progress).

### 🫥 Controls that get out of your way

All controls **float transparently over the video** — there's no black bar blocking your picture.

| Behaviour | Details |
|---|---|
| ⏱️ **Auto-hide** | Controls fade out **5 seconds** after your last touch or button press |
| 👆 **Touch to show** | Touch a video and *that tile's* controls come back |
| 🛡️ **Safe reveal** | The touch that **reveals** hidden controls will **never** accidentally press one |
| ✋ **Stays while in use** | Controls won't disappear while your finger is down (e.g. dragging a slider) |
| 🧩 **Per tile** | Every video has its own timer, so one hiding doesn't affect the others |

---

## 🎛️ Per-Video Controls

Every video tile has the same set of controls.

### ▶️ Core controls

| Control | What it does |
|---|---|
| ⏯️ **Play / Pause** | The big round button (centre of the tile, or bottom-left on small tiles) |
| 📏 **Progress bar** | Drag to seek; shows **elapsed** and **total** time |

### 🔊 Volume

| | |
|---|---|
| 🎚️ **Slider** | **0–100%**, with a live **percentage** readout |
| 🔇 **Mute button** | Mutes instantly — and **remembers your level** so un-muting restores it |
| 🎬 **Default** | Every video starts at **0%** so four soundtracks never collide |
| 🧠 **Smart saving** | At 0% the video's **audio decoder is switched off** entirely, saving resources |

### 🧰 The button grid

| Button | Name | What it does |
|:---:|---|---|
| 🔇 | **Mute** | Mute / un-mute, remembering your volume |
| 🔁 | **Loop** | Cycle **loop this video → loop folder → loop off** |
| ⏩ | **Speed** | Cycles **0.5× · 0.75× · 1× · 1.25× · 1.5× · 2×** — the button shows the current speed |
| 🔄 | **Rotate** | Turns the video **90° clockwise** each press. The picture is resized so it fills the tile properly |
| ⛶ | **Fill / Fit** | **Fill** crops the video to fill the tile and **removes the letterbox bars**; press again to **Fit** |
| ⇄ | **Swap** | Moves this video to another spot in the grid *(hidden when only one video)* |

### 🤏 Touch gestures

| Gesture | Result |
|---|---|
| **Pinch** | Zoom **1× – 5×** |
| **Drag** *(while zoomed)* | Pan around the picture |
| **Double-tap** | Reset zoom |

> 🎯 Pinch-zoom starts correctly **even if one finger lands on a button**, and zoom only affects the video — the controls never zoom with it.

### 📐 Smart on small screens

Four tiles on a phone means small tiles, so the controls adapt automatically:

- 🧩 The **button grid wraps** into as many rows as the tile's width allows.
- ▶️ If the buttons would crowd the centre play button, **play/pause moves to the bottom-left**.
- 🏷️ The **volume percentage label** hides on narrow tiles.
- ⏱️ The **time labels** hide on very narrow tiles so the progress bar keeps its room.

---

## 🔁 Loop Modes

Each video has its **own** loop button with **three modes**. Tap to cycle — a short message tells you where you landed.

| Icon | Mode | What happens |
|:---:|---|---|
| 🔂 | **Loop this video** *(default)* | The video repeats forever |
| 🔁 | **Loop the whole folder** | Plays **the next video in the same folder**, then the next… and after the **last one it wraps around to the first** |
| ⛔ | **Loop off** | Plays **once** and stops |

Cycle order: **🔂 this video → 🔁 whole folder → ⛔ off → 🔂 …**

### 📂 How "whole folder" works

- 🔤 Videos are ordered by **name, naturally** — so `clip2` comes **before** `clip10`.
- 🎞️ Only **video files directly in that folder** are included (not sub-folders).
- ♻️ The folder is **re-read each time** you switch to it, so newly added videos are picked up.
- 📍 Switching modes **keeps your current position** — there's no jump back to the start.
- ⭐ **Videos opened from the built-in browser** work instantly — no extra prompt.
- 🗂️ **Videos picked with the system file picker** need folder access once:
  1. The app asks you to **choose the folder** that contains the video.
  2. That access is **remembered**, so other videos from the same folder switch to folder-loop without asking again.
  3. If you pick a folder that doesn't contain the video, the app tells you and stays in the previous mode.

---

## 🔀 Swapping Videos

Want video 1 on the right and video 2 on the left? Use the **⇄ Swap** button.

| Videos | How swapping works |
|---|---|
| **2** | One press **swaps the two videos** instantly |
| **3 or 4** | Press **Swap**, then **choose where it goes** (see below) |
| **1** | Nothing to swap — the button is hidden |

### 🎯 Choosing a destination (3–4 videos)

1. Press **⇄ Swap** on the video you want to move.
2. Every **other** tile shows a big **"SWAP HERE"** pad.
3. **Tap** the one you want — or move to it with the remote and press **OK**.
4. The two videos trade places. 🎉

❌ **Changed your mind?** Press **Back**, or press **Swap** again, to cancel.

### 🧳 Everything travels with the video

Volume, speed, rotation, fill/fit, zoom, loop mode and **playback position** all move with it — and **both videos keep playing** during the swap.

---

## 📁 The Built-in Video Browser

A full **in-app file explorer** designed for **both touch and remote control**. It doesn't depend on any system file picker — which is exactly why it works on TVs that don't have one.

### 🌟 What it shows

For every video:

| | |
|---|---|
| 🖼️ **Thumbnail** | A frame from the video |
| 📛 **Name** | The file name |
| ⏱️ **Duration** | e.g. `12:34` or `1:02:11` |
| 📐 **Resolution** | e.g. `1920×1080` |
| 💾 **Size** | e.g. `845 MB` or `1.20 GB` |
| 📅 **Date** | When the file was last modified |

### 🧭 How it works

- 🏠 Starts at a **storage list**: **Internal storage**, any **USB drive / SD card** it can find, plus quick shortcuts to **Movies**, **Download** and **DCIM**.
- 📂 Open a folder to see its **sub-folders and videos**, sorted **naturally by name** (folders first).
- ⬆️ The top row, **"Up one level"**, takes you back — and **Back** on your remote does the same.
- 🧾 A **breadcrumb path** and **video count** sit at the top (e.g. *Internal storage › Movies — 12 videos*).
- ⚡ Thumbnails and details **load as you scroll**, so even huge folders open quickly.
- 📼 Recognises **all common video types**: MP4, M4V, MKV, WebM, MOV, AVI, 3GP, TS, MTS, MPG, WMV, FLV and more.
- 🙈 Hidden files and unreadable folders are skipped.

### 🎬 Two ways to use it

| Mode | How to open it | What happens |
|---|---|---|
| **📂 Browse mode** | Tap **BROWSE VIDEOS** on the splash screen | Pick a video and it **plays full screen on its own** |
| **🎯 Select mode** | Tap **SELECT** on a video slot *(on TVs, or anywhere the system picker is missing)* | Pick a video and it **fills that slot** |

### 🔐 First-time permission

The first time you open the browser, Android asks for permission to read videos. If the prompt doesn't appear (it can't on some TVs), the app shows **GRANT ACCESS** and **OPEN APP SETTINGS** buttons.

> 📱 **On phones and tablets**, the **SELECT** buttons use the regular Android file picker (so **Google Drive**, **Gallery** and friends keep working) — and fall back to the built-in browser if no system picker exists.

---

## 📺 Android TV & Fire TV (Remote Control)

Sub-Etha OmniView is built to be driven entirely by a **D-pad remote** — **no touchscreen needed**.

---

## ⚙️ Performance & Decoders

Playing four videos at once is demanding. Here's how the app keeps it smooth.

### 🧠 The TV hardware problem

Most TV chips allow only about **two hardware video decoders** at once (a tablet allows many) — so a naive player shows just two of four videos on a TV. Sub-Etha OmniView handles this for you:

| Technique | What it does |
|---|---|
| 🔎 **Hardware budget** | Asks the device how many hardware decoders it supports; that many videos start on **hardware**, the rest on **software** decoders (which have no such limit) |
| 🛟 **Automatic fallback** | If a decoder fails to start, ExoPlayer tries the next available one |
| 🩹 **Error recovery** | If a video's decoder errors out, **that video restarts on a software decoder**, keeping its position and settings |
| 🐕 **Stall watchdog** | Some TVs fail *silently* (no error, no picture). If a hardware video shows nothing after ~8 seconds, it switches to software too |
| 🔇 **Audio saving** | At **0% volume**, a video's audio decoder isn't used at all |
| 🧊 **TextureView rendering** | Smooth scaling, rotation and zoom for every tile |

### 💬 What to expect

- 🧮 **Software decoding uses the CPU.** Files at **1080p or lower** work best for the 3rd and 4th videos; 4K files may stutter on modest TVs.
- ⏳ A 3rd/4th video that needed recovery might start a few seconds late.
- 🎞️ Whether a particular file plays depends on the **codecs on your device**.
- 🧯 If a video can't be played even in software, you'll see a message with the error name instead of a silent blank tile.

---

## 🔐 Permissions

| Permission | Why |
|---|---|
| 📼 **Read videos** (`READ_MEDIA_VIDEO` on Android 13+, `READ_EXTERNAL_STORAGE` on older versions) | So the **built-in browser** can list and play videos on your storage |
| 🌐 **Internet** | Declared for network media support; local files don't need it |

> 🔒 The app **never uploads** your videos or collects any data.

---

## 🛠️ Building the App

**Requirements:** JDK 17 · Android Gradle Plugin 8.5.2 · Gradle 8.9+ · Kotlin 1.9.24 · `minSdk 24` (Android 7.0) · `targetSdk 34`

### 🅰️ Android Studio (easiest)

1. **Open** the project folder.
2. Let **Gradle sync** finish.
3. **Build ▸ Build APK(s)**.

### 🅱️ Command line

```bash
gradle assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

### 🅲 GitHub Actions (no setup on your computer)

Push the project to a GitHub repo. The included workflow (`.github/workflows/build.yml`) **builds the APK automatically** and attaches it as a downloadable **artifact**.

### 📲 Installing

- **Phone / tablet:** copy the APK over and open it (allow "install unknown apps").
- **Android TV / Fire TV:** sideload with `adb`:

```bash
adb connect <your-tv-ip>
adb install app-debug.apk
```

> ℹ️ This produces a **debug build** signed with the standard debug key — perfect for personal use and testing.

---

## 🗂️ Project Structure

```
QuadPlayer/
├── .github/workflows/build.yml      ⚙️  GitHub Actions build
├── app/src/main/
│   ├── AndroidManifest.xml          📜  TV support, permissions, activities
│   ├── java/com/example/quadplayer/
│   │   ├── SplashActivity.kt        🧭  Start screen, layout & video selection
│   │   ├── MainActivity.kt          🎬  Player screen, layouts, swapping, folder access
│   │   ├── BrowserActivity.kt       📁  In-app video explorer
│   │   ├── VideoTile.kt             🎛️  One video: player, controls, gestures, loop, speed…
│   │   ├── TileFrame.kt             🖐️  Touch / key / focus handling for a tile
│   │   ├── ButtonFlow.kt            🧩  Wrapping layout for the button grid
│   │   ├── TileHost.kt              🔌  Tile ⇄ screen communication
│   │   ├── LayoutMode.kt            🧱  The 9 layouts
│   │   ├── LayoutPreview.kt         🖼️  Splash-screen layout diagram
│   │   ├── DecoderPolicy.kt         ⚙️  Hardware / software decoder strategy
│   │   ├── VideoFiles.kt            🎞️  Video detection, natural sorting, formatting
│   │   ├── VideoMetaLoader.kt       🖼️  Thumbnails, duration & resolution loading
│   │   └── StorageRoots.kt          💾  Finds internal storage / USB / SD
│   └── res/                         🎨  Layouts, icons, dark + cyan theme, TV banner
└── README.md                        📖  You are here
```

---

## 🩺 Troubleshooting & Tips

<details>
<summary>🔇 <b>I can't hear any video!</b></summary>

Every video starts at **0%** on purpose. Tap a video, raise its **volume slider**, or press the **mute button** to un-mute.

</details>

<details>
<summary>📺 <b>Only some of my videos play on my TV.</b></summary>

TVs limit hardware decoders. The app switches extra videos to software decoding automatically — give a 3rd/4th video a few seconds to start, and prefer **1080p or lower** files for those.

</details>

<details>
<summary>🗂️ <b>The browser shows no videos or says it can't read a folder.</b></summary>

Make sure you've allowed **video / storage access** (use **OPEN APP SETTINGS** if needed). Only video files **directly inside** a folder are listed — open the sub-folder that holds them.

</details>

<details>
<summary>🔁 <b>Folder loop asked me to choose a folder.</b></summary>

That happens for videos picked with the **system file picker** (Android doesn't let apps peek at neighbouring files). Choose the folder that contains the video — it's **remembered** afterwards. Videos from the **built-in browser** never need this.

</details>

<details>
<summary>💽 <b>My USB drive isn't listed.</b></summary>

The app finds drives that Android makes visible to apps. If yours doesn't appear, it may be mounted in a way apps can't see on that device.

</details>

<details>
<summary>🎞️ <b>A video is listed but won't play.</b></summary>

Playback depends on your device's codecs. Some formats (older **AVI/WMV** files, for example) may not be supported on every device.

</details>

<details>
<summary>🔀 <b>Where did the Swap button go?</b></summary>

It only appears when you're playing **2 or more** videos.

</details>

<details>
<summary>⌨️ <b>Nothing happens when I press a remote button.</b></summary>

If the controls are hidden, the **first press just wakes them** — press again to use them.

</details>
