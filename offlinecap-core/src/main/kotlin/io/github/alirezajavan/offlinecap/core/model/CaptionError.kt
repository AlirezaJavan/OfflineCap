package io.github.alirezajavan.offlinecap.core.model

/**
 * Errors that can occur during the captioning pipeline.
 */
public sealed interface CaptionError {
    public data class ModelMissing(
        val model: WhisperModel,
    ) : CaptionError

    public data object UnsupportedMedia : CaptionError

    public data class DecodingFailed(
        val cause: Throwable?,
    ) : CaptionError

    public data class TranscriptionFailed(
        val cause: Throwable?,
    ) : CaptionError

    public data class TranslationFailed(
        val cause: Throwable?,
    ) : CaptionError

    public data object Cancelled : CaptionError
}
