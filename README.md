# OfflineCap

[![Build](https://github.com/alirezajavan/OfflineCap/actions/workflows/build.yml/badge.svg)](https://github.com/alirezajavan/OfflineCap/actions/workflows/build.yml)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

[![Maven Central (offlinecap)](https://img.shields.io/maven-central/v/io.github.alirezajavan/offlinecap?label=offlinecap)](https://central.sonatype.com/artifact/io.github.alirezajavan/offlinecap)
[![Maven Central (offlinecap-core)](https://img.shields.io/maven-central/v/io.github.alirezajavan/offlinecap-core?label=offlinecap-core)](https://central.sonatype.com/artifact/io.github.alirezajavan/offlinecap-core)
[![Maven Central (offlinecap-audio)](https://img.shields.io/maven-central/v/io.github.alirezajavan/offlinecap-audio?label=offlinecap-audio)](https://central.sonatype.com/artifact/io.github.alirezajavan/offlinecap-audio)
[![Maven Central (offlinecap-scribe)](https://img.shields.io/maven-central/v/io.github.alirezajavan/offlinecap-scribe?label=offlinecap-scribe)](https://central.sonatype.com/artifact/io.github.alirezajavan/offlinecap-scribe)
[![Maven Central (offlinecap-transcribe)](https://img.shields.io/maven-central/v/io.github.alirezajavan/offlinecap-transcribe?label=offlinecap-transcribe)](https://central.sonatype.com/artifact/io.github.alirezajavan/offlinecap-transcribe)
[![Maven Central (offlinecap-subtitle)](https://img.shields.io/maven-central/v/io.github.alirezajavan/offlinecap-subtitle?label=offlinecap-subtitle)](https://central.sonatype.com/artifact/io.github.alirezajavan/offlinecap-subtitle)
[![Maven Central (offlinecap-lingua)](https://img.shields.io/maven-central/v/io.github.alirezajavan/offlinecap-lingua?label=offlinecap-lingua)](https://central.sonatype.com/artifact/io.github.alirezajavan/offlinecap-lingua)

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
| `:offlinecap` | Public facade (`OfflineCap`, `ModelManager`); the one artifact for full video → subtitle |
| `:offlinecap-core` | Domain models, engine interfaces (incl. `SubtitleFormatter`), `CaptionPipeline` orchestrator |
| `:offlinecap-audio` | Audio extractor: MediaCodec-based video → PCM |
| `:offlinecap-scribe` | Whisper.cpp NDK engine (low-level; wrapped by `:offlinecap-transcribe`) |
| `:offlinecap-transcribe` | Audio → Transcript: `AudioTranscriber` (speech-to-text + Whisper model catalog/download) |
| `:offlinecap-subtitle` | Transcript → Subtitle: `SubtitleGenerator` (cue merging + SRT/WebVTT, configurable via `CueMergeOptions`) |
| `:offlinecap-lingua` | ML Kit Translate implementation |

Each module is independently versioned and can be depended on directly if you only need that
capability — see [Standalone modules](#standalone-modules) below.

## Installation

`:offlinecap` is the only artifact needed for the full video → subtitle pipeline; it pulls in the
audio, transcribe, subtitle, and lingua modules transitively.

```kotlin
dependencies {
    implementation("io.github.alirezajavan:offlinecap:<version>")
}
```

### Standalone modules

You don't need the whole library. Pick only the capability you need; each module below depends on
nothing beyond `:offlinecap-core` (and, for `:offlinecap-transcribe`, the low-level `:offlinecap-scribe`
engine it wraps) — no ML Kit, no subtitle writers, no facade pulled in unless you ask for them.
Every module has its own version and releases on its own cadence — bumping `offlinecap-transcribe`
doesn't force a new `offlinecap-audio` or `offlinecap` release, so pin whichever versions you're
actually on.

```kotlin
dependencies {
    // Audio extraction: video/audio file -> PCM
    implementation("io.github.alirezajavan:offlinecap-audio:<version>")

    // Speech-to-text + Whisper model download/catalog management
    implementation("io.github.alirezajavan:offlinecap-transcribe:<version>")

    // Transcript -> SRT/WebVTT formatting, with its own cue-merging options
    implementation("io.github.alirezajavan:offlinecap-subtitle:<version>")

    // On-device translation
    implementation("io.github.alirezajavan:offlinecap-lingua:<version>")
}
```

**Audio -> Transcript only** (`offlinecap-transcribe`) — no video decoding, no subtitle files, just
Whisper model management and transcription:

```kotlin
val transcriber = AudioTranscriber.Builder(context).build()

transcriber.models.state(WhisperModel.BASE).first().let { state ->
    if (state is ModelState.Missing) {
        transcriber.models.download(WhisperModel.BASE).collect { /* progress */ }
    }
}

transcriber.loadModel(WhisperModel.BASE)
transcriber.transcribe(pcmFlow, language = null).collect { event ->
    when (event) {
        is TranscriptionEvent.Progress -> { /* 0..1 */ }
        is TranscriptionEvent.Segment -> println(event.cue.text)
    }
}
transcriber.close()
```

If the input is a media file rather than already-decoded PCM, add `offlinecap-audio` too — still just
the two focused modules, no facade:

```kotlin
val pcmFlow = MediaCodecAudioDecoder(context).decode(mediaUri, PcmSpec())
transcriber.transcribe(pcmFlow, language = null).collect { /* ... */ }
```

**Transcript -> Subtitle only** (`offlinecap-subtitle`) — format a `Transcript` you already have,
with your own cue-merging options:

```kotlin
val generator = SubtitleGenerator(CueMergeOptions(maxCharsPerLine = 32, maxLines = 1))
val srt = generator.format(transcript, SubtitleFormat.SRT)
```

**Translation only** (`offlinecap-lingua`):

```kotlin
val translationEngine = MlKitTranslationEngine()
translationEngine.ensureModel(source = LanguageTag("en"), target = LanguageTag("fa")).collect { /* progress */ }
val translated = translationEngine.translate("Hello", source = LanguageTag("en"), target = LanguageTag("fa"))
```

## Sample app

`:sample` has two bottom-nav tabs that demonstrate the two ways to consume this library:

- **Caption** — uses the `:offlinecap` facade end-to-end (`OfflineCap.caption(request)`): pick a
  video, pick a Whisper model, optionally pick a target language, and `CaptionPipeline` runs
  extraction → transcription → translation → subtitle formatting as one orchestrated flow. This is
  what a real app depending on just `:offlinecap` would write.
- **Modules** — bypasses the facade and `CaptionPipeline` entirely, wiring `MediaCodecAudioDecoder`
  (`:offlinecap-audio`) → `AudioTranscriber` (`:offlinecap-transcribe`) → `SubtitleGenerator`
  (`:offlinecap-subtitle`) together by hand in `ModulesViewModel`, plus a standalone
  `MlKitTranslationEngine` (`:offlinecap-lingua`) text-translation demo. This is what you'd write if
  you only depended on the specific modules you need instead of the whole facade — see
  [Standalone modules](#standalone-modules) above for the same code outside the sample app.

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
