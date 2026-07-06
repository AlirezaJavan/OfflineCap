package io.github.alirezajavan.offlinecap.core.model

/**
 * Events emitted by the captioning pipeline.
 */
public sealed interface CaptionEvent {
    public data class ExtractingAudio(
        val progress: Float,
    ) : CaptionEvent

    public data class Transcribing(
        val progress: Float,
        val latest: SubtitleCue?,
    ) : CaptionEvent

    public data class Translating(
        val progress: Float,
    ) : CaptionEvent

    public data class Completed(
        val result: CaptionResult,
    ) : CaptionEvent

    public data class Failed(
        val error: CaptionError,
    ) : CaptionEvent
}
