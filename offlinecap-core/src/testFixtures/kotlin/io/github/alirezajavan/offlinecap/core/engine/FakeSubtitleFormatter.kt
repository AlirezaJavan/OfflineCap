package io.github.alirezajavan.offlinecap.core.engine

import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import io.github.alirezajavan.offlinecap.core.model.SubtitleFormat
import io.github.alirezajavan.offlinecap.core.model.Transcript

public class FakeSubtitleFormatter : SubtitleFormatter {
    override fun mergeCues(rawSegments: List<SubtitleCue>): List<SubtitleCue> = rawSegments

    override fun format(
        transcript: Transcript,
        format: SubtitleFormat,
    ): String = "FAKE:$format:${transcript.cues.joinToString(",") { it.text }}"
}
