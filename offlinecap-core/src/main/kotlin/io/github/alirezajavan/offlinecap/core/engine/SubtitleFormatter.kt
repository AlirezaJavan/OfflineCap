package io.github.alirezajavan.offlinecap.core.engine

import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import io.github.alirezajavan.offlinecap.core.model.SubtitleFormat
import io.github.alirezajavan.offlinecap.core.model.Transcript

/**
 * Interface for turning raw transcription segments into subtitle text.
 */
public interface SubtitleFormatter {
    /**
     * Merges raw ASR segments into readable subtitle cues.
     */
    public fun mergeCues(rawSegments: List<SubtitleCue>): List<SubtitleCue>

    /**
     * Renders [transcript] as subtitle text in the given [format].
     */
    public fun format(
        transcript: Transcript,
        format: SubtitleFormat,
    ): String
}
