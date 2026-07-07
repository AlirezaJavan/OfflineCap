package io.github.alirezajavan.offlinecap.subtitle

/**
 * Options controlling how raw ASR segments are merged into readable subtitle cues.
 */
public data class CueMergeOptions(
    public val maxCharsPerLine: Int = 42,
    public val maxLines: Int = 2,
)
