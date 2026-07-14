package io.github.alirezajavan.offlinecap.subtitle

import com.google.common.truth.Truth.assertThat
import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import io.github.alirezajavan.offlinecap.core.model.Transcript
import io.github.alirezajavan.offlinecap.core.model.WordTiming
import org.junit.jupiter.api.Test

class JsonTranscriptWriterTest {
    private val writer = JsonTranscriptWriter()

    @Test
    fun `write produces valid pretty-printed JSON`() {
        val transcript =
            Transcript(
                cues =
                    listOf(
                        SubtitleCue(
                            index = 1,
                            startMs = 0,
                            endMs = 1500,
                            text = "Hello world",
                            words =
                                listOf(
                                    WordTiming("Hello", 0, 500, 0.99f),
                                    WordTiming("world", 600, 1500, 0.98f),
                                ),
                        ),
                    ),
                language = LanguageTag("en"),
            )

        val json = writer.write(transcript)

        // Basic structural checks
        assertThat(json).contains("\"language\": \"en\"")
        assertThat(json).contains("\"text\": \"Hello world\"")
        assertThat(json).contains("\"words\": [")
        assertThat(json).contains("\"confidence\": 0.99")

        // Pretty print check
        assertThat(json).contains("\n")
    }

    @Test
    fun `write handles empty transcript`() {
        val transcript = Transcript(emptyList(), LanguageTag("fa"))
        val json = writer.write(transcript)

        assertThat(json).contains("\"cues\": []")
        assertThat(json).contains("\"language\": \"fa\"")
    }
}
