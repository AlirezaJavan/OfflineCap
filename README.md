# OfflineCap

[![Build](https://github.com/alirezajavan/OfflineCap/actions/workflows/build.yml/badge.svg)](https://github.com/alirezajavan/OfflineCap/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.alirezajavan/offlinecap)](https://central.sonatype.com/artifact/io.github.alirezajavan/offlinecap)

Offline video captioning library for Android.

Video in → audio extracted → speech transcribed on-device (whisper.cpp) → translated on-device (ML Kit) → subtitle file out (SRT/WebVTT).

100% offline at runtime (models are downloaded once, on demand).

## Features

- **MediaCodec Powered**: Uses Android platform decoders for hardware-accelerated audio extraction with zero binary overhead.
- **Whisper.cpp**: Fast, on-device speech-to-text transcription.
- **ML Kit Translate**: On-device neural machine translation.
- **Streaming Pipeline**: Windowed processing for bounded memory usage, regardless of video length.
- **Clean Architecture**: Pure Kotlin core logic with swappable engine implementations.

## Modules

| Module | Role |
|---|---|
| `:offlinecap` | Public facade (`OfflineCap`, `ModelManager`); the one artifact consumers add |
| `:offlinecap-core` | Domain models, engine interfaces, subtitle writers, `CaptionPipeline` orchestrator |
| `:offlinecap-audio` | MediaCodec-based audio extraction |
| `:offlinecap-scribe` | Whisper.cpp NDK implementation |
| `:offlinecap-lingua` | ML Kit Translate implementation |

## Installation

`:offlinecap` is the only artifact consumers need to add; it pulls in the audio, scribe, and lingua engines transitively.

```kotlin
dependencies {
    implementation("io.github.alirezajavan:offlinecap:<version>")
}
```

## Quick Start

### 1. Initialize

```kotlin
val offlineCap = OfflineCap.Builder(context)
    .transcriptionModel(WhisperModel.BASE)
    .build()
```

### 2. Manage Models

Models must be present locally before processing.

```kotlin
offlineCap.models.state(WhisperModel.BASE).collect { state ->
    if (state is ModelState.Missing) {
        offlineCap.models.download(WhisperModel.BASE).collect { /* progress */ }
    }
}
```

### 3. Start Captioning

```kotlin
val request = CaptionRequest(
    videoUri = "content://...",
    targetLanguage = LanguageTag("fa") // Optional translation
)

offlineCap.caption(request).collect { event ->
    when (event) {
        is CaptionEvent.ExtractingAudio -> { /* ... */ }
        is CaptionEvent.Transcribing -> { /* ... */ }
        is CaptionEvent.Completed -> {
            println(event.result.subtitleContent)
        }
        is CaptionEvent.Failed -> { /* ... */ }
    }
}
```

## License

Apache License 2.0
