# OfflineCap — Feature Roadmap

> **Purpose of this file.** A step-by-step, self-contained plan of candidate features any AI
> coding agent can pick up and implement without further context. Each phase is independent
> unless a dependency is stated. Work in phase order *within* a phase; phases themselves can be
> tackled in any order the maintainer prioritizes.
>
> **How to use this file (read before touching code):**
> 1. Read the root `CLAUDE.md` first — it is the source of truth for architecture, module
>    boundaries, code conventions, testing, and versioning. This roadmap never overrides it.
> 2. Pick a phase. Do every step in its checklist. **Mark each `[ ]` as `[x]` as you finish it.**
> 3. Respect the **Verification Gate** at the end of each phase — do not mark a phase "Done"
>    until the gate is green.
> 4. Every phase includes an explicit **Sample app** step: no feature is complete until it is
>    demonstrated in `:sample`.
> 5. Bump only the changed module's `VERSION_<MODULE_NAME>` in root `gradle.properties`
>    (see CLAUDE.md → Versioning). Never edit versions of modules you didn't touch.
>
> **Global rules that apply to every phase (from CLAUDE.md — restated so they aren't missed):**
> - Kotlin only, `explicitApi()`, KDoc on every public symbol, functions ≤ ~25 lines.
> - `:offlinecap-core` and `:offlinecap-subtitle` must stay pure JVM — **never** add an
>   `androidx`/`com.android` dependency to them.
> - Engines are interfaces in core; implementations live in their own module; `CaptionPipeline`
>   touches interfaces only (DIP). Manual DI in the `:offlinecap` Builder.
> - Async = coroutines + cold `Flow`. No callbacks/listeners in public API. Rethrow
>   `CancellationException`. Release every native/OS resource in `finally`/`use`/`onCompletion`.
> - Errors are values: extend `CaptionError` (sealed) rather than throwing across the pipeline.
> - Versions live only in `gradle/libs.versions.toml`; never hardcode a version in a build file.
> - A change is not done until its tests pass and `./gradlew build spotlessCheck` is green.
> - Do NOT implement anything in the *Deferred / Out of Scope* section at the bottom.

---

## Status board

Update this table as phases complete. It is the at-a-glance index; the per-phase checklists are the detail.

| # | Phase | Primary module(s) | New permissions | Status |
|---|-------|-------------------|-----------------|--------|
| 1 | Word-level timestamps | core, scribe, transcribe | — | ☑ Done |
| 2 | JSON transcript export | core, subtitle | — | ☐ Not started |
| 3 | ASS/SSA subtitle format | core, subtitle | — | ☐ Not started |
| 4 | Automatic source-language detection | core, scribe, transcribe | — | ☐ Not started |
| 5 | Voice-activity detection (skip silence) | core, audio | — | ☐ Not started |
| 6 | Live microphone captioning | audio (+ new mic source), offlinecap | `RECORD_AUDIO` | ☐ Not started |
| 7 | Background captioning service + progress notification | offlinecap, sample | `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PROCESSING`, `POST_NOTIFICATIONS` | ☐ Not started |
| 8 | WiFi-only / background model downloads | transcribe, offlinecap | `ACCESS_NETWORK_STATE` | ☐ Not started |
| 9 | Export subtitles to shared storage (MediaStore) | sample (+ optional helper) | — (scoped storage) | ☐ Not started |

Status legend: ☐ Not started · ◐ In progress · ☑ Done.

---

## Phase 1 — Word-level timestamps

**Why.** whisper.cpp can emit per-token timestamps. Exposing them enables karaoke-style highlighting,
tighter cue splitting, and better WebVTT. Pure data-model + engine work, no permissions.

**Files to touch**
- `offlinecap-core/.../model/SubtitleCue.kt` (or `Transcript.kt`) — add an optional
  `words: List<WordTiming>` field; add a new `WordTiming(text, startMs, endMs, confidence?)` model.
- `offlinecap-scribe/.../WhisperJni.kt` + native glue — surface token timestamps from whisper
  (`whisper_full_get_token_data` / `whisper_full_n_tokens`). `WhisperJni` stays declaration-only.
- `offlinecap-scribe/.../WhisperTranscriptionEngine.kt` — map tokens → `WordTiming`.
- `offlinecap-scribe/.../WhisperDecodeOptions.kt` — add `wordTimestamps: Boolean = false` flag.

**Steps**
- [x] Add `WordTiming` model + optional `words` on the cue/segment; keep it nullable/empty-default so existing consumers are source-compatible.
- [x] Add `wordTimestamps` to `WhisperDecodeOptions` (default `false`, off = zero overhead).
- [x] Extend the JNI layer to read token timings; ensure every `Get*ArrayElements` has a matching `Release*`.
- [x] Populate `words` in `WhisperTranscriptionEngine` only when the flag is on.
- [x] **Sample app:** in the Modules tab (or Caption transcript card), when word timings are present, render the active cue with per-word chips/highlighting to prove the data flows through.
- [x] Unit tests: `WhisperTranscriptionEngineTest` maps a fake token stream to `WordTiming`; core model test for the new type. Use fakes from `testFixtures`, not the real native lib.
- [x] Bump `VERSION_OFFLINECAP_CORE`, `VERSION_OFFLINECAP_SCRIBE` (no `:offlinecap-transcribe` files changed, so its version is left untouched per CLAUDE.md versioning policy).

**Verification Gate**
- [x] `./gradlew :offlinecap-core:test :offlinecap-transcribe:test spotlessCheck` green.
- [x] `./gradlew dokkaGenerate` clean (new public symbols documented).
- [x] Sample builds: `./gradlew :sample:assembleDebug`.

**Phase 1 Done:** [x]

---

## Phase 2 — JSON transcript export

**Why.** Downstream tools (editors, search indexes) want structured output, not just SRT/WebVTT.
Pure `:offlinecap-subtitle` work; no Android, no permissions. Pairs well with Phase 1 (emit word timings in JSON).

**Files to touch**
- `offlinecap-core/.../model/SubtitleFormat.kt` — add `JSON` enum entry.
- `offlinecap-subtitle/.../JsonTranscriptWriter.kt` (new) — implement the writer.
- `offlinecap-subtitle/.../SubtitleGenerator.kt` — route `SubtitleFormat.JSON` to the new writer.

**Steps**
- [ ] Add `JSON` to `SubtitleFormat`; handle it exhaustively everywhere the enum is matched (compiler will flag the `when`s).
- [ ] Implement `JsonTranscriptWriter` with a hand-rolled or `kotlinx.serialization` writer — if adding a dependency, declare it in `libs.versions.toml`; keep the module pure JVM (kotlinx-serialization is fine, it is not Android).
- [ ] Include cue text, start/end ms, and (if present) word timings from Phase 1.
- [ ] **Sample app:** add a "JSON" choice to the export format and an "Export JSON" action; wire a `CreateDocument("application/json")` launcher next to the existing SRT export.
- [ ] Unit tests: `JsonTranscriptWriterTest` — golden-string comparison for a known transcript, including the empty-transcript edge case.
- [ ] Bump `VERSION_OFFLINECAP_CORE`, `VERSION_OFFLINECAP_SUBTITLE`.

**Verification Gate**
- [ ] `./gradlew :offlinecap-subtitle:test :offlinecap-core:test spotlessCheck` green.
- [ ] `./gradlew :sample:assembleDebug`.

**Phase 2 Done:** [ ]

---

## Phase 3 — ASS/SSA subtitle format (styled subtitles)

**Why.** ASS/SSA is the standard for styled/positioned subtitles used by media players. Pure subtitle work.

**Files to touch**
- `offlinecap-core/.../model/SubtitleFormat.kt` — add `ASS`.
- `offlinecap-subtitle/.../AssWriter.kt` (new) — minimal valid `[Script Info]` / `[V4+ Styles]` / `[Events]` output.
- `offlinecap-subtitle/.../SubtitleGenerator.kt` — route `ASS`.
- Optionally a small `SubtitleStyle` value type in core (font, size, color) fed via `CueMergeOptions` or a new options type — keep it pure-JVM (no `android.graphics.Color`; use a plain ARGB `Int`/hex string).

**Steps**
- [ ] Add `ASS` enum + exhaustive handling.
- [ ] Implement `AssWriter` producing a spec-valid file with one default style; correct ASS timestamp format (`H:MM:SS.cs`, centiseconds).
- [ ] (Optional) add `SubtitleStyle` and thread it through `SubtitleGenerator`.
- [ ] **Sample app:** add "ASS" to the export format choices and an export action.
- [ ] Unit tests: `AssWriterTest` golden output + timestamp-formatting edge cases (0 ms, >1 h).
- [ ] Bump `VERSION_OFFLINECAP_CORE`, `VERSION_OFFLINECAP_SUBTITLE`.

**Verification Gate**
- [ ] `./gradlew :offlinecap-subtitle:test spotlessCheck` green; `:sample:assembleDebug`.

**Phase 3 Done:** [ ]

---

## Phase 4 — Automatic source-language detection

**Why.** Today `sourceLanguage` must be supplied or defaulted. whisper can auto-detect. Removes a
manual step and improves multi-language videos.

**Files to touch**
- `offlinecap-scribe/.../WhisperTranscriptionEngine.kt` — when source language is null, call
  whisper's language auto-detect (`whisper_lang_auto_detect` / set `language = "auto"`), and report
  the detected language.
- `offlinecap-core/.../engine/TranscriptionEvent.kt` — add a `LanguageDetected(tag, confidence)` event (or add the field to an existing progress event).
- `offlinecap-core/.../model/CaptionEvent.kt` / `CaptionResult.kt` — surface detected source language to the facade consumer.
- `offlinecap-core/.../pipeline/CaptionPipeline.kt` — propagate detected language into the translation step so translation source is correct.

**Steps**
- [ ] Add the detected-language signal to the engine event model (keep events sealed/exhaustive).
- [ ] Auto-detect in `WhisperTranscriptionEngine` only when `sourceLanguage == null`.
- [ ] Feed detected source language into `TranslationEngine.ensureModel/translate` in the pipeline.
- [ ] Add detected language to `CaptionResult` so UIs can display it.
- [ ] **Sample app:** show "Detected language: xx" in the Progress/Subtitle card once available.
- [ ] Tests: `CaptionPipelineTest` — with `FakeTranscriptionEngine` emitting a detected language, assert it reaches `CaptionResult` and drives translation source. Extend fakes in `testFixtures` as needed.
- [ ] Bump `VERSION_OFFLINECAP_CORE`, `VERSION_OFFLINECAP_SCRIBE`, `VERSION_OFFLINECAP_TRANSCRIBE`.

**Verification Gate**
- [ ] `./gradlew :offlinecap-core:test :offlinecap-transcribe:test spotlessCheck` green; `:sample:assembleDebug`.

**Phase 4 Done:** [ ]

---

## Phase 5 — Voice-activity detection (skip silence)

**Why.** Skipping silent windows speeds up transcription and avoids hallucinated cues on silence.
Lives in `:offlinecap-audio` operating on PCM windows — pure signal processing, no permissions.

**Files to touch**
- `offlinecap-audio/.../Vad.kt` (new) — energy/RMS-threshold gate over `PcmChunk`/windows (a simple
  gate first; a WebRTC/Silero VAD is Deferred).
- `offlinecap-core/.../model/PcmSpec.kt` or a new `VadOptions` in core — enable flag + threshold.
- Wire VAD as an optional stage between decoder output and transcriber input (in `AudioTranscriber`
  or `CaptionPipeline` — keep the filter in `:offlinecap-audio`, the option type in core).

**Steps**
- [ ] Add `VadOptions(enabled, energyThreshold, minSilenceMs)` value type in core.
- [ ] Implement the RMS/energy gate as a `Flow<PcmChunk> -> Flow<PcmChunk>` operator; drop windows below threshold, preserve timestamps so cue timing stays correct.
- [ ] Make it opt-in and off by default (no behavior change unless enabled).
- [ ] **Sample app:** add a "Skip silence (VAD)" toggle in the Modules tab and pass the option through.
- [ ] Tests: `VadTest` — synthetic loud/quiet PCM windows in, assert quiet windows dropped and timestamps preserved.
- [ ] Bump `VERSION_OFFLINECAP_CORE`, `VERSION_OFFLINECAP_AUDIO`.

**Verification Gate**
- [ ] `./gradlew :offlinecap-audio:test :offlinecap-core:test spotlessCheck` green; `:sample:assembleDebug`.

**Phase 5 Done:** [ ]

---

## Phase 6 — Live microphone captioning  ⚠️ new permission

**Why.** Real-time captioning from the mic (not just files) is a headline capability and the main
reason to request `RECORD_AUDIO`. Reuses the existing PCM → transcribe path.

**New permission:** `android.permission.RECORD_AUDIO` (runtime permission, must be requested at
runtime on API 26+). Declare in the module's `AndroidManifest.xml` **and** in the sample; the
library must not assume it is granted — surface a typed error if not.

**Files to touch**
- `offlinecap-audio/.../MicrophoneAudioSource.kt` (new) — `AudioRecord` → 16 kHz mono float PCM
  `Flow`, wrapped as `AutoCloseable`, released in `finally`/`onCompletion`. Mirrors the
  `MediaCodecAudioDecoder` output contract so `AudioTranscriber` consumes it unchanged.
- `offlinecap-core/.../model/CaptionError.kt` — add `PermissionDenied(permission: String)`.
- `offlinecap/.../OfflineCap.kt` — add a `captionLive()` entry point (or a `CaptionRequest` source
  variant) that reads from the mic instead of a URI.

**Steps**
- [ ] Implement `MicrophoneAudioSource` producing the same PCM spec the transcriber expects; handle `AudioRecord` init failure as a value, not a crash.
- [ ] Add `CaptionError.PermissionDenied`; emit `CaptionEvent.Failed(PermissionDenied("android.permission.RECORD_AUDIO"))` when the permission is missing (check via `ContextCompat.checkSelfPermission`).
- [ ] Add `OfflineCap.captionLive(...)` returning a cold `Flow<CaptionEvent>`; stop cleanly on collector cancellation (release `AudioRecord`).
- [ ] Declare `RECORD_AUDIO` in the audio module manifest (and merge into `:offlinecap`).
- [ ] **Sample app:** add a third bottom-nav tab **"Live"** — request `RECORD_AUDIO` at runtime, start/stop live captioning, stream cues into the existing transcript card. Add the permission to the sample manifest.
- [ ] Tests: JVM-testable pieces (permission-denied path, flow cancellation/cleanup) with fakes; real `AudioRecord` capture is an `@LargeTest` instrumented test (CI-optional).
- [ ] Bump `VERSION_OFFLINECAP_CORE`, `VERSION_OFFLINECAP_AUDIO`, `VERSION_OFFLINECAP`.

**Verification Gate**
- [ ] `./gradlew build spotlessCheck` green (unit tests only; instrumented tests optional).
- [ ] `:sample:assembleDebug`, permission requested at runtime, denial handled gracefully.

**Phase 6 Done:** [ ]

---

## Phase 7 — Background captioning service + progress notification  ⚠️ new permissions

**Why.** Long videos should keep processing when the app is backgrounded, with a progress
notification the user can watch/cancel. This is the canonical foreground-service use case.

**New permissions:**
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MEDIA_PROCESSING` (API 34+ typed FGS)
- `android.permission.POST_NOTIFICATIONS` (runtime, API 33+)

> Design note: keep the service in the **sample app**, not the library, so the library stays a
> pure dependency with no forced service/notification policy. If a reusable helper is wanted,
> expose only a small `CaptionForegroundService` scaffold that the app subclasses — decide during
> implementation; default is sample-only.

**Files to touch**
- `sample/.../CaptionForegroundService.kt` (new) — foreground service that collects
  `OfflineCap.caption(...)` and posts progress to a notification; supports a Cancel action.
- `sample/src/main/AndroidManifest.xml` — declare the service with
  `foregroundServiceType="mediaProcessing"` and the three permissions above.
- `sample/.../CaptionViewModel.kt` — start/stop the service; reflect its state in the UI.

**Steps**
- [ ] Create the foreground service; start it typed as `mediaProcessing`; build an ongoing notification with determinate progress and a Cancel action.
- [ ] Request `POST_NOTIFICATIONS` at runtime (API 33+) before starting; degrade gracefully if denied (still process, just no notification).
- [ ] Route captioning through the service so progress survives backgrounding; stop + remove notification on completion/cancel/failure.
- [ ] **Sample app:** add a "Run in background" toggle on the Caption tab that dispatches to the service; show notification-permission rationale.
- [ ] Tests: Robolectric test that the service starts foreground and posts a notification; ViewModel state transitions. Native/real processing stays out of unit tests.
- [ ] No library version bumps unless a helper is added to `:offlinecap` (then bump `VERSION_OFFLINECAP`).

**Verification Gate**
- [ ] `./gradlew :sample:assembleDebug` and any Robolectric tests green; `./gradlew build spotlessCheck` green.
- [ ] Manual: start a long job, background the app, confirm the notification updates and Cancel works.

**Phase 7 Done:** [ ]

---

## Phase 8 — WiFi-only / background model downloads

**Why.** Whisper/ML Kit models are large; users want "download over WiFi only" and downloads that
survive app death. Improves the existing `ModelRepository.download` path.

**New permission:** `android.permission.ACCESS_NETWORK_STATE` (normal permission, no runtime prompt)
to observe connectivity/metering.

**Files to touch**
- `offlinecap-transcribe/.../WhisperModelRepository.kt` — accept a `DownloadConstraints`
  (e.g. `requireUnmetered: Boolean`) and honor it; optionally support resumable downloads.
- `offlinecap-core/.../engine/ModelRepository.kt` — extend the `download` contract with optional
  constraints (keep the default overload source-compatible).
- (Optional) a `WorkManager`-backed downloader in `:offlinecap` for survive-app-death behavior —
  declare WorkManager in `libs.versions.toml`; keep it in an Android module only.

**Steps**
- [ ] Add `DownloadConstraints(requireUnmetered = false)` in core; thread through `download(...)` with a backward-compatible default.
- [ ] In `WhisperModelRepository`, check metering via `ConnectivityManager`/`NetworkCapabilities` and fail fast with a typed error (or wait) when unmetered is required but unavailable.
- [ ] Declare `ACCESS_NETWORK_STATE` in the transcribe module manifest.
- [ ] (Optional) WorkManager path for background downloads.
- [ ] **Sample app:** add a "WiFi-only downloads" switch in the Model card; pass the constraint into `download`.
- [ ] Tests: `WhisperModelRepositoryTest` with MockWebServer + a fake connectivity checker — assert an unmetered-required download is blocked on metered and proceeds on unmetered.
- [ ] Bump `VERSION_OFFLINECAP_CORE`, `VERSION_OFFLINECAP_TRANSCRIBE` (and `VERSION_OFFLINECAP` if the optional WorkManager path lands there).

**Verification Gate**
- [ ] `./gradlew :offlinecap-transcribe:test :offlinecap-core:test spotlessCheck` green; `:sample:assembleDebug`.

**Phase 8 Done:** [ ]

---

## Phase 9 — Export subtitles to shared storage (MediaStore)

**Why.** Today the sample exports via the system `CreateDocument` picker (good, no permission). This
phase adds an optional "Save to Movies/Subtitles" convenience using MediaStore on API 29+
(scoped storage — **no** `WRITE_EXTERNAL_STORAGE` needed on modern APIs). Primarily a sample-app improvement.

**Files to touch**
- `sample/.../SubtitleExporter.kt` (new) — write the subtitle string via `MediaStore` into a
  shared collection on API 29+; keep the existing SAF picker as the default.
- `sample/.../CaptionViewModel.kt` / `MainActivity.kt` — add the "Save to device" action.

**Steps**
- [ ] Implement MediaStore-based save (API 29+); fall back to SAF on older APIs. Do not request legacy storage permissions.
- [ ] **Sample app:** add a "Save to device" button alongside "Export SRT"; show the resulting path/toast.
- [ ] Tests: Robolectric test of the exporter's content-values/URI construction where feasible.
- [ ] No library version bumps (sample-only) unless a helper is promoted into a library module.

**Verification Gate**
- [ ] `./gradlew :sample:assembleDebug` green; `./gradlew build spotlessCheck` green.

**Phase 9 Done:** [ ]

---

## Permissions summary

| Permission | Introduced in | Runtime prompt? | Why |
|------------|---------------|-----------------|-----|
| `RECORD_AUDIO` | Phase 6 | Yes | Live microphone captioning |
| `POST_NOTIFICATIONS` | Phase 7 | Yes (API 33+) | Background job progress notification |
| `FOREGROUND_SERVICE` | Phase 7 | No | Run captioning as a foreground service |
| `FOREGROUND_SERVICE_MEDIA_PROCESSING` | Phase 7 | No | Typed FGS (API 34+) |
| `ACCESS_NETWORK_STATE` | Phase 8 | No | Detect metered/WiFi for download constraints |
| `INTERNET` | (already present) | No | One-time model downloads |

Photo/video picking uses the Android Photo Picker (`PickVisualMedia`) which needs **no** storage
permission — keep it that way; do not add `READ_MEDIA_VIDEO`/`READ_EXTERNAL_STORAGE`.

---

## Deferred / Out of Scope (do NOT implement without a new decision)

- **Subtitle burn-in / re-muxing into the video** (requires an encoder/muxer pipeline; large surface).
- **Speaker diarization** (no lightweight on-device path today).
- **Cloud/remote transcription or translation fallback** — violates the "fully offline at runtime" promise.
- **Silero/WebRTC neural VAD** — Phase 5 ships a simple energy gate first; neural VAD is a later, separate decision.
- **A DI framework** — manual DI in the Builder only (per CLAUDE.md).
- **Streaming/partial word-level rendering as its own module** — fold into Phase 1 if wanted.
