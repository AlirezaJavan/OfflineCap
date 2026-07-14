package io.github.alirezajavan.offlinecap.core.model

import kotlinx.serialization.Serializable

/**
 * The timing and text of a single recognized word within a [SubtitleCue].
 */
@Serializable
public data class WordTiming(
    public val text: String,
    public val startMs: Long,
    public val endMs: Long,
    public val confidence: Float? = null,
) {
    init {
        require(startMs >= 0) { "Start time must be non-negative" }
        require(endMs >= startMs) { "End time must be greater than or equal to start time" }
    }
}
