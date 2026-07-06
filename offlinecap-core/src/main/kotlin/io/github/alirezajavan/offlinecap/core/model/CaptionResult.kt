package io.github.alirezajavan.offlinecap.core.model

/**
 * The final result of a successful captioning process.
 */
public data class CaptionResult(
    public val transcript: Transcript,
    public val translatedTranscript: Transcript?,
    public val subtitleContent: String,
)
