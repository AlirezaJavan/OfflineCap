package io.github.alirezajavan.offlinecap.core.model

import kotlinx.serialization.Serializable

/**
 * A single subtitle cue with timing and text.
 */
@Serializable
public data class SubtitleCue(
    public val index: Int,
    public val startMs: Long,
    public val endMs: Long,
    public val text: String,
    public val words: List<WordTiming> = emptyList(),
) {
    init {
        require(index >= 0) { "Index must be non-negative" }
        require(startMs >= 0) { "Start time must be non-negative" }
        require(endMs >= startMs) { "End time must be greater than or equal to start time" }
    }
}
