package io.github.alirezajavan.offlinecap.core.pipeline

import io.github.alirezajavan.offlinecap.core.engine.AudioDecoder
import io.github.alirezajavan.offlinecap.core.engine.SubtitleFormatter
import io.github.alirezajavan.offlinecap.core.engine.TranscriptionEngine
import io.github.alirezajavan.offlinecap.core.engine.TranscriptionEvent
import io.github.alirezajavan.offlinecap.core.engine.TranslationEngine
import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.CaptionError
import io.github.alirezajavan.offlinecap.core.model.CaptionEvent
import io.github.alirezajavan.offlinecap.core.model.CaptionRequest
import io.github.alirezajavan.offlinecap.core.model.CaptionResult
import io.github.alirezajavan.offlinecap.core.model.PcmSpec
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import io.github.alirezajavan.offlinecap.core.model.Transcript
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onEach

public class CaptionPipeline(
    private val audioDecoder: AudioDecoder,
    private val transcriptionEngine: TranscriptionEngine,
    private val translationEngine: TranslationEngine,
    private val subtitleFormatter: SubtitleFormatter,
) {
    public fun execute(request: CaptionRequest): Flow<CaptionEvent> =
        channelFlow {
            try {
                // 1. Decode Audio (progress derived from each window's position vs. total duration)
                send(CaptionEvent.ExtractingAudio(0f))
                val pcmFlow =
                    audioDecoder
                        .decode(request.videoUri, PcmSpec())
                        .onEach { chunk ->
                            if (chunk.totalDurationMs > 0) {
                                val progress = (chunk.startMs.toFloat() / chunk.totalDurationMs).coerceIn(0f, 1f)
                                send(CaptionEvent.ExtractingAudio(progress))
                            }
                        }
                        // Decode ahead while whisper is busy; without a buffer the two
                        // stages run strictly serialized and total time is their sum.
                        .buffer(capacity = 2)

                // 2. Transcribe
                val rawCues = mutableListOf<SubtitleCue>()
                var detectedLanguage = request.sourceLanguage
                var latestProgress = 0f

                transcriptionEngine.transcribe(pcmFlow, request.sourceLanguage).collect { event ->
                    when (event) {
                        is TranscriptionEvent.Progress -> {
                            latestProgress = event.progress
                            send(CaptionEvent.Transcribing(latestProgress, rawCues.lastOrNull()))
                        }
                        is TranscriptionEvent.Segment -> {
                            rawCues.add(event.cue)
                            send(CaptionEvent.Transcribing(latestProgress, event.cue))
                        }
                    }
                }

                val sourceTranscript =
                    Transcript(
                        cues = subtitleFormatter.mergeCues(rawCues),
                        language = detectedLanguage ?: LanguageTag("en"),
                    )

                // 3. Optional Translation (progress per cue translated)
                val targetLang = request.targetLanguage
                val finalTranscript =
                    if (targetLang != null && targetLang != sourceTranscript.language) {
                        val totalCues = sourceTranscript.cues.size
                        val translatedCues =
                            sourceTranscript.cues.mapIndexed { index, cue ->
                                send(CaptionEvent.Translating(index / totalCues.toFloat()))
                                val translatedText =
                                    translationEngine.translate(
                                        text = cue.text,
                                        source = sourceTranscript.language,
                                        target = targetLang,
                                    )
                                cue.copy(text = translatedText)
                            }
                        send(CaptionEvent.Translating(1f))
                        Transcript(translatedCues, targetLang)
                    } else {
                        null
                    }

                // 4. Format Output
                val subtitleContent = subtitleFormatter.format(finalTranscript ?: sourceTranscript, request.format)

                send(
                    CaptionEvent.Completed(
                        CaptionResult(
                            transcript = sourceTranscript,
                            translatedTranscript = finalTranscript,
                            subtitleContent = subtitleContent,
                        ),
                    ),
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                send(CaptionEvent.Failed(mapError(e)))
            }
        }

    private fun mapError(e: Exception): CaptionError =
        when (e) {
            is IllegalStateException -> CaptionError.TranscriptionFailed(e)
            else -> CaptionError.DecodingFailed(e)
        }
}
