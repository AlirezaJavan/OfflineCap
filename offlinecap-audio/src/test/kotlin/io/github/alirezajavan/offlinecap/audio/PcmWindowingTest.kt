package io.github.alirezajavan.offlinecap.audio

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.alirezajavan.offlinecap.core.model.PcmChunk
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PcmWindowingTest {
    @Test
    fun `windowing splits large stream into fixed chunks`() =
        runTest {
            // 1 second of audio at 16kHz
            val samples = FloatArray(16_000)
            val upstream = listOf(PcmChunk(samples, 0)).asFlow()

            // Window is 30s, so 1s should come out as a single partial window
            PcmWindowing.window(upstream).test {
                val chunk = awaitItem()
                assertThat(chunk.samples.size).isEqualTo(16_000)
                awaitComplete()
            }
        }

    @Test
    fun `windowing emits overlapping windows with correct starts and content`() =
        runTest {
            // 65 seconds at 16kHz; sample value encodes its absolute index
            val samples = FloatArray(16_000 * 65) { it.toFloat() }
            val upstream = listOf(PcmChunk(samples, 0, 65_000)).asFlow()

            PcmWindowing.window(upstream).test {
                val first = awaitItem()
                assertThat(first.startMs).isEqualTo(0)
                assertThat(first.samples.size).isEqualTo(16_000 * 30)
                assertThat(first.samples.first()).isEqualTo(0f)

                // Step is 29s (30s window minus 1s overlap)
                val second = awaitItem()
                assertThat(second.startMs).isEqualTo(29_000)
                assertThat(second.samples.size).isEqualTo(16_000 * 30)
                assertThat(second.samples.first()).isEqualTo((16_000 * 29).toFloat())

                val tail = awaitItem()
                assertThat(tail.startMs).isEqualTo(58_000)
                assertThat(tail.samples.size).isEqualTo(16_000 * 7)
                assertThat(tail.samples.first()).isEqualTo((16_000 * 58).toFloat())

                awaitComplete()
            }
        }
}
