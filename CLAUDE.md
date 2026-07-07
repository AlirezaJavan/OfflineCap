# OfflineCap

Offline video captioning **library** for Android: video → audio extraction (MediaCodec) → on-device speech-to-text (whisper.cpp/JNI) → on-device translation (ML Kit) → SRT/WebVTT. Fully offline at runtime; models downloaded once on demand.

**The implementation is driven by `docs/PLAN.md`.** Before doing anything, read it, work in phase order, respect Verification Gates, and mark checkboxes `[x]` as you complete steps. Do not implement anything listed in its *Deferred / Out of Scope* section.

## Modules

| Module | Type | Role |
|---|---|---|
| `:offlinecap-core` | Kotlin/JVM (no Android!) | Domain models, engine interfaces (incl. `SubtitleFormatter`), language mapping, `CaptionPipeline` orchestrator |
| `:offlinecap-audio` | Android lib | Audio extractor: `MediaCodecAudioDecoder` → 16 kHz mono float PCM windows. Standalone-usable. |
| `:offlinecap-scribe` | Android lib + NDK | whisper.cpp JNI, `WhisperTranscriptionEngine` — low-level engine, wrapped by `:offlinecap-transcribe` |
| `:offlinecap-transcribe` | Android lib | Audio → Transcript: `AudioTranscriber` (wraps `WhisperTranscriptionEngine` + `WhisperModelRepository`, the whisper model catalog/downloader). Standalone-usable. |
| `:offlinecap-subtitle` | Kotlin/JVM (no Android!) | Transcript → Subtitle: `SubtitleGenerator` (cue merging via `CueMergeOptions` + SRT/WebVTT writers). Standalone-usable. |
| `:offlinecap-lingua` | Android lib | ML Kit Translate, `MlKitTranslationEngine` |
| `:offlinecap` | Android lib | Public facade (`OfflineCap`, `ModelManager`) composing all of the above; the one artifact for full video → subtitle |
| `:sample` | Android app | Compose demo; depends only on `:offlinecap` |
| `build-logic` | included build | Convention plugins (`offlinecap.kotlin.jvm`, `offlinecap.android.library`, `offlinecap.android.application`, `offlinecap.android.compose`, `offlinecap.publish`) |

Dependency rule: engines depend on core via `api`; core depends on nothing Android. Never add an `androidx`/`com.android` dependency to `:offlinecap-core` or `:offlinecap-subtitle`.

**Versioning:** every publishable module has its own `VERSION_<MODULE_NAME>` entry in root
`gradle.properties` (e.g. `:offlinecap-core` → `VERSION_OFFLINECAP_CORE`), derived automatically
from the Gradle project name by `PublishConventionPlugin` — no per-module `build.gradle.kts` edits
needed. `VERSION_NAME` is only a fallback default for a module that doesn't have its own entry yet
(e.g. right after scaffolding it). **When a module's files change, bump only that module's
`VERSION_<MODULE_NAME>`** — leave every other module's version untouched, even if it's a
dependency of the one that changed (consumers pin the exact version they need via Gradle
coordinates; they don't need to re-pull an unchanged transitive dependency).

## Build & Verify

```bash
./gradlew build                 # compile + all unit tests, all modules
./gradlew :offlinecap-core:test # fastest feedback loop (pure JVM)
./gradlew spotlessCheck         # ktlint via Spotless (spotlessApply to fix)
./gradlew dokkaGenerate         # KDoc must build clean
./gradlew :sample:assembleDebug
./gradlew publishToMavenLocal
```

Toolchain: Gradle 9.6.1 wrapper, AGP 9.2.1, Kotlin 2.4.0, JDK 17, compileSdk 36 / minSdk 26, NDK + CMake for `:offlinecap-scribe`. Versions live only in `gradle/libs.versions.toml`; never hardcode a version in a build file. Module build files stay declarative and short — shared config belongs in a convention plugin.

## Code Conventions

- Kotlin only. `explicitApi()` in every library module: explicit visibility + return types, KDoc on every public symbol.
- **Architecture:** engines are interfaces defined in core (`AudioDecoder`, `TranscriptionEngine`, `TranslationEngine`, `ModelRepository`); implementations live in their module; `CaptionPipeline` only touches interfaces (DIP). Constructor injection, manual DI in the `:offlinecap` Builder — no DI framework.
- **CQS:** queries pure and value-returning; commands return `Unit`/`Result`/`Flow`. No compute-and-mutate methods.
- **Async:** coroutines + cold `Flow` only; no callbacks/listeners in public API. Suspend functions are main-safe. `CancellationException` is always rethrown. Every native/OS resource released in `finally`/`use`/`onCompletion`.
- **Errors as values:** the pipeline emits `CaptionEvent.Failed(CaptionError)`; sealed, typed, exhaustive. Exceptions only for programmer errors (`require`/`check`).
- **Size limits:** functions ≤ ~25 lines, classes ≤ ~250 lines, one responsibility each. Comments say *why*, not *what*. No commented-out code, no TODO without an issue reference.
- **JNI:** all native handles wrapped in `AutoCloseable` classes; `WhisperJni` contains only `external fun` declarations (no logic); every `Get*ArrayElements` has a matching `Release*`.
- **Naming:** impls prefixed by technology (`WhisperTranscriptionEngine`), fakes `Fake*` in `testFixtures`, flows named as values (`events`), suspend commands as verbs.

## Testing

- JUnit 5 (`useJUnitPlatform()`), Truth assertions, Turbine for Flow, `kotlinx-coroutines-test` (virtual time — never `Thread.sleep` in tests), MockK, MockWebServer, Robolectric for Android glue.
- Prefer fakes (from module `testFixtures`) over mocks for behavioral collaborators.
- Real-device/native tests are `@LargeTest` instrumented tests and are CI-optional; everything else must run on the JVM.
- A change is not done until its tests pass and `./gradlew build spotlessCheck` is green.

## Git

- Conventional commits: `feat(core): …`, `build: …`, `test(scribe): …`, `docs: …`.
- Small commits; at minimum one commit per plan phase. Never commit `local.properties`, model `.bin` files, or `.cxx/`.
