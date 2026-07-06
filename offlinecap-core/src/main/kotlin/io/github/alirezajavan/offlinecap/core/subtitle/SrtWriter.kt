package io.github.alirezajavan.offlinecap.core.subtitle

import io.github.alirezajavan.offlinecap.core.model.Transcript

/**
 * SubRip (SRT) subtitle writer.
 */
internal class SrtWriter : SubtitleWriter {
    override fun write(transcript: Transcript): String =
        buildString {
            transcript.cues.forEachIndexed { index, cue ->
                append(index + 1)
                append("\n")
                append(formatTimestamp(cue.startMs))
                append(" --> ")
                append(formatTimestamp(cue.endMs))
                append("\n")
                append(cue.text.trim())
                append("\n\n")
            }
        }.trimEnd() + "\n"

    private fun formatTimestamp(ms: Long): String {
        val hours = ms / 3_600_000
        val minutes = (ms % 3_600_000) / 60_000
        val seconds = (ms % 60_000) / 1_000
        val milliseconds = ms % 1_000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, milliseconds)
    }
}
