package io.github.alirezajavan.offlinecap.core.subtitle

import io.github.alirezajavan.offlinecap.core.model.SubtitleCue

/**
 * Utility for merging raw ASR segments into readable subtitle cues.
 */
public object CueMerger {
    private const val MAX_CHARS_PER_LINE = 42
    private const val MAX_LINES = 2
    private const val MAX_CUE_CHARS = MAX_CHARS_PER_LINE * MAX_LINES

    /**
     * Merges segments based on character count and line limits.
     */
    public fun merge(rawSegments: List<SubtitleCue>): List<SubtitleCue> {
        if (rawSegments.isEmpty()) return emptyList()

        val mergedCues = mutableListOf<SubtitleCue>()
        var currentText = StringBuilder()
        var currentStart = rawSegments.first().startMs
        var currentIndex = 0

        rawSegments.forEach { segment ->
            val nextText = if (currentText.isEmpty()) segment.text.trim() else "$currentText ${segment.text.trim()}"

            if (nextText.length > MAX_CUE_CHARS) {
                // Emit current
                mergedCues.add(SubtitleCue(currentIndex++, currentStart, segment.startMs, currentText.toString()))
                currentText = StringBuilder(segment.text.trim())
                currentStart = segment.startMs
            } else {
                currentText = StringBuilder(nextText)
            }
        }

        // Final chunk
        if (currentText.isNotEmpty()) {
            mergedCues.add(SubtitleCue(currentIndex, currentStart, rawSegments.last().endMs, currentText.toString()))
        }

        return mergedCues
    }
}
