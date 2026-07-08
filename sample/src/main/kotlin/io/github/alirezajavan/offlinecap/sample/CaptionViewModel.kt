package io.github.alirezajavan.offlinecap.sample

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.alirezajavan.offlinecap.OfflineCap
import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.CaptionEvent
import io.github.alirezajavan.offlinecap.core.model.CaptionRequest
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import io.github.alirezajavan.offlinecap.core.model.WhisperModel
import io.github.alirezajavan.offlinecap.scribe.WhisperDecodeOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "OfflineCapSample"
private const val ETA_MIN_SAMPLE_MS = 5_000L
private const val ETA_TICK_MS = 1_000L

enum class CaptionStage { IDLE, EXTRACTING, TRANSCRIBING, TRANSLATING, COMPLETED, FAILED }

data class UiState(
    val whisperModel: WhisperModel = WhisperModel.TINY,
    val activeModel: WhisperModel? = null,
    val modelState: ModelState = ModelState.Missing,
    val targetLanguage: LanguageTag? = null,
    val wordTimestampsEnabled: Boolean = false,
    val stage: CaptionStage = CaptionStage.IDLE,
    val extractionProgress: Float = 0f,
    val transcriptionProgress: Float = 0f,
    val cues: List<SubtitleCue> = emptyList(),
    val translatedCues: List<SubtitleCue> = emptyList(),
    val subtitleContent: String? = null,
    val errorMessage: String? = null,
    val isProcessing: Boolean = false,
    val remainingTimeMs: Long? = null,
    val elapsedMs: Long? = null,
)

class CaptionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private var offlineCap = buildOfflineCap(model = WhisperModel.TINY, wordTimestamps = false)
    private var modelObserverJob: Job? = null
    private var captioningJob: Job? = null
    private var etaTickerJob: Job? = null
    private var startedAtMs: Long = 0L
    private var transcribingStartedAtMs: Long? = null
    private var latestTranscribingProgress: Float = 0f
    private var lastLoggedPercent: Int = -1

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        observeModelState()
    }

    private fun buildOfflineCap(
        model: WhisperModel,
        wordTimestamps: Boolean,
    ): OfflineCap =
        OfflineCap
            .Builder(getApplication())
            .transcriptionModel(model)
            .transcriptionOptions(WhisperDecodeOptions(wordTimestamps = wordTimestamps))
            .build()

    private fun observeModelState() {
        modelObserverJob?.cancel()
        modelObserverJob =
            viewModelScope.launch {
                offlineCap.models.state(_uiState.value.whisperModel).collectLatest { state ->
                    _uiState.value = _uiState.value.copy(modelState = state)
                }
            }
    }

    fun setModel(model: WhisperModel) {
        if (model == _uiState.value.whisperModel) return
        offlineCap = buildOfflineCap(model, _uiState.value.wordTimestampsEnabled)
        _uiState.value = _uiState.value.copy(whisperModel = model)
        observeModelState()
    }

    fun setTargetLanguage(language: LanguageTag?) {
        _uiState.value = _uiState.value.copy(targetLanguage = language)
    }

    fun setWordTimestampsEnabled(enabled: Boolean) {
        if (enabled == _uiState.value.wordTimestampsEnabled) return
        offlineCap = buildOfflineCap(_uiState.value.whisperModel, enabled)
        _uiState.value = _uiState.value.copy(wordTimestampsEnabled = enabled)
        observeModelState()
    }

    fun downloadModel() {
        viewModelScope.launch {
            offlineCap.models.download(_uiState.value.whisperModel).collect { state ->
                _uiState.value = _uiState.value.copy(modelState = state)
            }
        }
    }

    fun startCaptioning(videoUri: String) {
        val model = _uiState.value.whisperModel
        startedAtMs = System.currentTimeMillis()
        transcribingStartedAtMs = null
        latestTranscribingProgress = 0f
        lastLoggedPercent = -1
        Log.i(TAG, "caption start model=${model.modelName} uri=$videoUri")
        captioningJob =
            viewModelScope.launch {
                _uiState.value =
                    UiState(
                        whisperModel = model,
                        activeModel = model,
                        modelState = _uiState.value.modelState,
                        targetLanguage = _uiState.value.targetLanguage,
                        stage = CaptionStage.EXTRACTING,
                        isProcessing = true,
                    )
                startEtaTicker()
                // Passing the language explicitly skips whisper's auto-detect pass (~seconds).
                val request =
                    CaptionRequest(
                        videoUri = videoUri,
                        sourceLanguage = LanguageTag("en"),
                        targetLanguage = _uiState.value.targetLanguage,
                    )
                offlineCap.caption(request).collect { event ->
                    logEvent(event)
                    _uiState.value = reduce(_uiState.value, event)
                    if (event is CaptionEvent.Completed || event is CaptionEvent.Failed) {
                        stopEtaTicker()
                        _uiState.value =
                            _uiState.value.copy(
                                isProcessing = false,
                                activeModel = null,
                                remainingTimeMs = null,
                                elapsedMs = System.currentTimeMillis() - startedAtMs,
                            )
                    }
                }
            }
    }

    private fun reduce(
        state: UiState,
        event: CaptionEvent,
    ): UiState =
        when (event) {
            is CaptionEvent.ExtractingAudio ->
                state.copy(
                    // Extraction streams in parallel with transcription; don't regress the stage label.
                    stage = if (state.stage == CaptionStage.TRANSCRIBING) state.stage else CaptionStage.EXTRACTING,
                    extractionProgress = maxOf(state.extractionProgress, event.progress),
                )
            is CaptionEvent.Transcribing -> {
                val cue = event.latest
                if (cue != null && transcribingStartedAtMs == null) {
                    transcribingStartedAtMs = System.currentTimeMillis()
                }
                latestTranscribingProgress = maxOf(latestTranscribingProgress, event.progress)
                state.copy(
                    stage = CaptionStage.TRANSCRIBING,
                    transcriptionProgress = latestTranscribingProgress,
                    cues = if (cue != null && cue != state.cues.lastOrNull()) state.cues + cue else state.cues,
                )
            }
            is CaptionEvent.Translating -> state.copy(stage = CaptionStage.TRANSLATING)
            is CaptionEvent.Completed ->
                state.copy(
                    stage = CaptionStage.COMPLETED,
                    extractionProgress = 1f,
                    transcriptionProgress = 1f,
                    translatedCues =
                        event.result.translatedTranscript
                            ?.cues
                            .orEmpty(),
                    subtitleContent = event.result.subtitleContent,
                )
            is CaptionEvent.Failed ->
                state.copy(
                    stage = CaptionStage.FAILED,
                    errorMessage = event.error.toString(),
                )
        }

    private fun logEvent(event: CaptionEvent) {
        val elapsed = System.currentTimeMillis() - startedAtMs
        when (event) {
            is CaptionEvent.ExtractingAudio ->
                Log.d(TAG, "extracting ${(event.progress * 100).toInt()}% (+${elapsed}ms)")
            is CaptionEvent.Transcribing -> {
                val percent = (event.progress * 100).toInt()
                val cue = event.latest
                if (cue != null && cue != _uiState.value.cues.lastOrNull()) {
                    Log.i(TAG, "segment [${cue.startMs}..${cue.endMs}ms] \"${cue.text.trim()}\" (+${elapsed}ms)")
                } else if (percent / 5 != lastLoggedPercent / 5) {
                    lastLoggedPercent = percent
                    Log.d(TAG, "transcribing $percent% (+${elapsed}ms)")
                }
            }
            is CaptionEvent.Translating ->
                Log.d(TAG, "translating ${(event.progress * 100).toInt()}% (+${elapsed}ms)")
            is CaptionEvent.Completed ->
                Log.i(TAG, "completed in ${elapsed}ms, ${event.result.transcript.cues.size} cues")
            is CaptionEvent.Failed ->
                Log.e(TAG, "failed after ${elapsed}ms: ${event.error}")
        }
    }

    fun stopCaptioning() {
        captioningJob?.cancel()
        stopEtaTicker()
        Log.i(TAG, "caption cancelled after ${System.currentTimeMillis() - startedAtMs}ms")
        _uiState.value =
            _uiState.value.copy(
                isProcessing = false,
                activeModel = null,
                remainingTimeMs = null,
                stage = CaptionStage.IDLE,
            )
    }

    private fun startEtaTicker() {
        etaTickerJob =
            viewModelScope.launch {
                while (true) {
                    delay(ETA_TICK_MS)
                    val startedAt = transcribingStartedAtMs ?: continue
                    val progress = latestTranscribingProgress
                    val elapsedMs = System.currentTimeMillis() - startedAt
                    if (elapsedMs < ETA_MIN_SAMPLE_MS || progress <= 0.01f) continue
                    val estimatedTotalMs = elapsedMs / progress
                    val rawRemainingMs = (estimatedTotalMs - elapsedMs).toLong().coerceAtLeast(0)
                    // Blend toward the raw estimate from a counting-down baseline so the
                    // display ticks down between progress updates instead of inflating
                    // while a window is still being decoded/transcribed.
                    val baselineMs =
                        _uiState.value.remainingTimeMs
                            ?.minus(ETA_TICK_MS)
                            ?.coerceAtLeast(0)
                    val remainingMs =
                        if (baselineMs == null) rawRemainingMs else (baselineMs * 0.7 + rawRemainingMs * 0.3).toLong()
                    _uiState.value = _uiState.value.copy(remainingTimeMs = remainingMs)
                }
            }
    }

    private fun stopEtaTicker() {
        etaTickerJob?.cancel()
        etaTickerJob = null
    }

    fun saveSubtitle(uri: Uri) {
        val content = _uiState.value.subtitleContent ?: return
        viewModelScope.launch {
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray())
            }
        }
    }
}
