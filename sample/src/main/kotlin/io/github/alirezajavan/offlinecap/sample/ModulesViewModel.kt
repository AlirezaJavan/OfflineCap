package io.github.alirezajavan.offlinecap.sample

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.alirezajavan.offlinecap.audio.MediaCodecAudioDecoder
import io.github.alirezajavan.offlinecap.core.engine.TranscriptionEvent
import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.PcmSpec
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import io.github.alirezajavan.offlinecap.core.model.SubtitleFormat
import io.github.alirezajavan.offlinecap.core.model.Transcript
import io.github.alirezajavan.offlinecap.core.model.WhisperModel
import io.github.alirezajavan.offlinecap.lingua.MlKitTranslationEngine
import io.github.alirezajavan.offlinecap.subtitle.CueMergeOptions
import io.github.alirezajavan.offlinecap.subtitle.SubtitleGenerator
import io.github.alirezajavan.offlinecap.transcribe.AudioTranscriber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "OfflineCapModulesDemo"
private val TRANSCRIBE_MODEL = WhisperModel.TINY
private val TRANSCRIBE_SOURCE_LANGUAGE = LanguageTag("en")

/**
 * Drives the "Modules" tab, which exercises [AudioTranscriber] (`:offlinecap-transcribe`),
 * [SubtitleGenerator] (`:offlinecap-subtitle`) and [MlKitTranslationEngine] (`:offlinecap-lingua`)
 * directly, without the `:offlinecap` facade or `CaptionPipeline`.
 */
data class ModulesUiState(
    val modelState: ModelState = ModelState.Missing,
    val isTranscribing: Boolean = false,
    val rawCues: List<SubtitleCue> = emptyList(),
    val mergeOptions: CueMergeOptions = CueMergeOptions(),
    val subtitleFormat: SubtitleFormat = SubtitleFormat.SRT,
    val subtitleContent: String? = null,
    val transcribeError: String? = null,
    val targetLanguage: LanguageTag = LanguageTag("fa"),
    val translationInput: String = "The weather is nice today.",
    val isTranslating: Boolean = false,
    val translationOutput: String? = null,
    val translationError: String? = null,
)

class ModulesViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val transcriber = AudioTranscriber.Builder(getApplication()).build()
    private val audioDecoder = MediaCodecAudioDecoder(getApplication())
    private val translationEngine = MlKitTranslationEngine()

    private val _uiState = MutableStateFlow(ModulesUiState())
    val uiState: StateFlow<ModulesUiState> = _uiState.asStateFlow()

    private var transcribeJob: Job? = null
    private var translateJob: Job? = null

    init {
        viewModelScope.launch {
            transcriber.models.state(TRANSCRIBE_MODEL).collectLatest { state ->
                _uiState.value = _uiState.value.copy(modelState = state)
            }
        }
    }

    fun downloadModel() {
        viewModelScope.launch {
            transcriber.models.download(TRANSCRIBE_MODEL).collect { state ->
                _uiState.value = _uiState.value.copy(modelState = state)
            }
        }
    }

    fun transcribe(mediaUri: String) {
        transcribeJob?.cancel()
        transcribeJob =
            viewModelScope.launch {
                _uiState.value =
                    _uiState.value.copy(
                        isTranscribing = true,
                        rawCues = emptyList(),
                        subtitleContent = null,
                        transcribeError = null,
                    )
                try {
                    transcriber.loadModel(TRANSCRIBE_MODEL)
                    val pcmFlow = audioDecoder.decode(mediaUri, PcmSpec())
                    val cues = mutableListOf<SubtitleCue>()
                    transcriber.transcribe(pcmFlow, TRANSCRIBE_SOURCE_LANGUAGE).collect { event ->
                        if (event is TranscriptionEvent.Segment) {
                            cues += event.cue
                            _uiState.value = _uiState.value.copy(rawCues = cues.toList())
                            regenerateSubtitle()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "transcribe failed", e)
                    _uiState.value = _uiState.value.copy(transcribeError = e.message ?: e.toString())
                } finally {
                    _uiState.value = _uiState.value.copy(isTranscribing = false)
                }
            }
    }

    fun setMergeOptions(options: CueMergeOptions) {
        _uiState.value = _uiState.value.copy(mergeOptions = options)
        regenerateSubtitle()
    }

    fun setSubtitleFormat(format: SubtitleFormat) {
        _uiState.value = _uiState.value.copy(subtitleFormat = format)
        regenerateSubtitle()
    }

    private fun regenerateSubtitle() {
        val state = _uiState.value
        if (state.rawCues.isEmpty()) return
        val generator = SubtitleGenerator(state.mergeOptions)
        val transcript =
            Transcript(
                cues = generator.mergeCues(state.rawCues),
                language = TRANSCRIBE_SOURCE_LANGUAGE,
            )
        _uiState.value = _uiState.value.copy(subtitleContent = generator.format(transcript, state.subtitleFormat))
    }

    fun setTranslationInput(text: String) {
        _uiState.value = _uiState.value.copy(translationInput = text)
    }

    fun setTargetLanguage(language: LanguageTag) {
        _uiState.value = _uiState.value.copy(targetLanguage = language)
    }

    fun translate() {
        translateJob?.cancel()
        val state = _uiState.value
        translateJob =
            viewModelScope.launch {
                _uiState.value = state.copy(isTranslating = true, translationOutput = null, translationError = null)
                try {
                    translationEngine.ensureModel(TRANSCRIBE_SOURCE_LANGUAGE, state.targetLanguage).collect()
                    val translated =
                        translationEngine.translate(
                            text = state.translationInput,
                            source = TRANSCRIBE_SOURCE_LANGUAGE,
                            target = state.targetLanguage,
                        )
                    _uiState.value = _uiState.value.copy(translationOutput = translated)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "translate failed", e)
                    _uiState.value = _uiState.value.copy(translationError = e.message ?: e.toString())
                } finally {
                    _uiState.value = _uiState.value.copy(isTranslating = false)
                }
            }
    }

    override fun onCleared() {
        transcriber.close()
        translationEngine.close()
    }
}
