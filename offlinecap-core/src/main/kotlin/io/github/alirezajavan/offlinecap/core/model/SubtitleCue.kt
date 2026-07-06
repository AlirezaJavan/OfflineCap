package io.github.alirezajavan.offlinecap.core.model

/**
 * A single subtitle cue with timing and text.
 */
public data class SubtitleCue(
    public val index: Int,
    public val startMs: Long,
    public val endMs: Long,
    public val text: String,
) {
    init {
        require(index >= 0) { "Index must be non-negative" }
        require(startMs >= 0) { "Start time must be non-negative" }
        require(endMs >= startMs) { "End time must be greater than or equal to start time" }
    }
}
