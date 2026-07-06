package io.github.alirezajavan.offlinecap.core.subtitle

import io.github.alirezajavan.offlinecap.core.model.SubtitleFormat
import io.github.alirezajavan.offlinecap.core.model.Transcript

/**
 * Interface for writing a [Transcript] to a subtitle string.
 */
public interface SubtitleWriter {
    public fun write(transcript: Transcript): String

    public companion object {
        public fun forFormat(format: SubtitleFormat): SubtitleWriter =
            when (format) {
                SubtitleFormat.SRT -> SrtWriter()
                SubtitleFormat.WEBVTT -> WebVttWriter()
            }
    }
}
