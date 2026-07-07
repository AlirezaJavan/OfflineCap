package io.github.alirezajavan.offlinecap.subtitle

import com.google.common.truth.Truth.assertThat
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import org.junit.jupiter.api.Test

class CueMergerTest {
    @Test
    fun `merge returns empty list for empty input`() {
        assertThat(CueMerger.merge(emptyList())).isEmpty()
    }

    @Test
    fun `merge combines short segments into one cue`() {
        val segments =
            listOf(
                SubtitleCue(0, 0, 500, "Hello"),
                SubtitleCue(1, 500, 1000, "world"),
            )

        val merged = CueMerger.merge(segments)

        assertThat(merged).hasSize(1)
        assertThat(merged[0].text).isEqualTo("Hello world")
        assertThat(merged[0].startMs).isEqualTo(0)
        assertThat(merged[0].endMs).isEqualTo(1000)
    }

    @Test
    fun `merge splits into a new cue once maxCharsPerLine times maxLines is exceeded`() {
        val options = CueMergeOptions(maxCharsPerLine = 5, maxLines = 1)
        val segments =
            listOf(
                SubtitleCue(0, 0, 500, "Hello"),
                SubtitleCue(1, 500, 1000, "world"),
            )

        val merged = CueMerger.merge(segments, options)

        assertThat(merged).hasSize(2)
        assertThat(merged[0].text).isEqualTo("Hello")
        assertThat(merged[1].text).isEqualTo("world")
    }
}
