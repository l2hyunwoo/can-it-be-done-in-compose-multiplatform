# Can it be done in Compose Multiplatform?

A collection of graphics & animation demos rebuilt in **Compose Multiplatform**, inspired by
the *"Can it be done in Jetpack Compose?"* series — but running on **Android, iOS, and Desktop**
from a single Kotlin codebase (wasmJs added per-demo where the graphics APIs allow).

Each demo is a **self-contained CMP project** in its own directory, so you can open, build, and
run any one of them independently.

## Demos

| Demo | Android | iOS | Desktop | wasmJs |
|------|:-------:|:---:|:-------:|:------:|
| [wave-loading](wave-loading/) — animated liquid wave fill | ✅ | ✅ | ✅ | — |

## Structure

```
can-it-be-done-in-compose-multiplatform/
├── README.md
├── wave-loading/            # independent CMP project
│   ├── composeApp/          # shared Compose code + Android/Desktop/wasm entrypoints
│   ├── iosApp/              # Xcode project
│   ├── settings.gradle.kts
│   └── gradlew
└── <next-demo>/             # same layout
```

## Running a demo

```bash
cd wave-loading

# Desktop (fastest iteration loop)
./gradlew :composeApp:run

# Android (device/emulator connected)
./gradlew :composeApp:installDebug

# iOS — open iosApp/iosApp.xcodeproj in Xcode and run
```

## Versions

Kotlin `2.4.0` · Compose Multiplatform `1.11.1` · AGP `9.2.1` · JDK 17+.
