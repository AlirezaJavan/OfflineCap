package io.github.alirezajavan.offlinecap.subtitle

import io.github.alirezajavan.offlinecap.core.model.SubtitleCue

/**
 * Utility for merging raw ASR segments into readable subtitle cues.
 */
public object CueMerger {
    /**
     * Merges segments based on [options]' character count and line limits.
     */
    public fun merge(
        rawSegments: List<SubtitleCue>,
        options: CueMergeOptions = CueMergeOptions(),
    ): List<SubtitleCue> {
        if (rawSegments.isEmpty()) return emptyList()
        val maxCueChars = options.maxCharsPerLine * options.maxLines

        val mergedCues = mutableListOf<SubtitleCue>()
        var currentText = StringBuilder()
        var currentStart = rawSegments.first().startMs
        var currentIndex = 0

        rawSegments.forEach { segment ->
            val nextText = if (currentText.isEmpty()) segment.text.trim() else "$currentText ${segment.text.trim()}"

            if (nextText.length > maxCueChars) {
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
