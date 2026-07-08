package io.github.alirezajavan.offlinecap.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class WordTimingTest {
    @Test
    fun `constructs with valid timings`() {
        val word = WordTiming(text = "hi", startMs = 0, endMs = 100, confidence = 0.5f)
        assertThat(word.text).isEqualTo("hi")
        assertThat(word.confidence).isEqualTo(0.5f)
    }

    @Test
    fun `confidence defaults to null`() {
        val word = WordTiming(text = "hi", startMs = 0, endMs = 100)
        assertThat(word.confidence).isNull()
    }

    @Test
    fun `rejects negative start time`() {
        assertThrows<IllegalArgumentException> {
            WordTiming(text = "hi", startMs = -1, endMs = 100)
        }
    }

    @Test
    fun `rejects end time before start time`() {
        assertThrows<IllegalArgumentException> {
            WordTiming(text = "hi", startMs = 100, endMs = 50)
        }
    }

    @Test
    fun `subtitle cue defaults to no words`() {
        val cue = SubtitleCue(index = 0, startMs = 0, endMs = 100, text = "hi")
        assertThat(cue.words).isEmpty()
    }
}
