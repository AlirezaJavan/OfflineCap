package io.github.alirezajavan.offlinecap.subtitle

import com.google.common.truth.Truth.assertThat
import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import io.github.alirezajavan.offlinecap.core.model.SubtitleFormat
import io.github.alirezajavan.offlinecap.core.model.Transcript
import org.junit.jupiter.api.Test

class SubtitleGeneratorTest {
    private val transcript =
        Transcript(
            cues = listOf(SubtitleCue(0, 0, 1_500, "Hello world")),
            language = LanguageTag("en"),
        )

    @Test
    fun `format renders SRT`() {
        val srt = SubtitleGenerator().format(transcript, SubtitleFormat.SRT)

        assertThat(srt).contains("1\n00:00:00,000 --> 00:00:01,500\nHello world")
    }

    @Test
    fun `format renders WebVTT`() {
        val vtt = SubtitleGenerator().format(transcript, SubtitleFormat.WEBVTT)

        assertThat(vtt).startsWith("WEBVTT\n\n")
        assertThat(vtt).contains("00:00:00.000 --> 00:00:01.500\nHello world")
    }

    @Test
    fun `format renders JSON`() {
        val json = SubtitleGenerator().format(transcript, SubtitleFormat.JSON)

        assertThat(json).contains("\"text\": \"Hello world\"")
        assertThat(json).contains("\"language\": \"en\"")
    }

    @Test
    fun `mergeCues delegates to CueMerger with the configured options`() {
        val generator = SubtitleGenerator(CueMergeOptions(maxCharsPerLine = 5, maxLines = 1))
        val rawSegments =
            listOf(
                SubtitleCue(0, 0, 500, "Hello"),
                SubtitleCue(1, 500, 1000, "world"),
            )

        val merged = generator.mergeCues(rawSegments)

        assertThat(merged).hasSize(2)
    }
}
