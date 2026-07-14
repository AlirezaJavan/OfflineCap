package io.github.alirezajavan.offlinecap.subtitle

import io.github.alirezajavan.offlinecap.core.engine.SubtitleFormatter
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import io.github.alirezajavan.offlinecap.core.model.SubtitleFormat
import io.github.alirezajavan.offlinecap.core.model.Transcript

/**
 * Transcript-to-subtitle facade: merges raw ASR segments into readable cues and renders them as
 * SRT or WebVTT text. Usable standalone (a client with its own [Transcript] can call [format]
 * directly) or as the [SubtitleFormatter] plugged into `CaptionPipeline`.
 */
public class SubtitleGenerator(
    private val options: CueMergeOptions = CueMergeOptions(),
) : SubtitleFormatter {
    override fun mergeCues(rawSegments: List<SubtitleCue>): List<SubtitleCue> = CueMerger.merge(rawSegments, options)

    override fun format(
        transcript: Transcript,
        format: SubtitleFormat,
    ): String =
        when (format) {
            SubtitleFormat.SRT -> SrtWriter()
            SubtitleFormat.WEBVTT -> WebVttWriter()
            SubtitleFormat.JSON -> JsonTranscriptWriter()
        }.write(transcript)
}
