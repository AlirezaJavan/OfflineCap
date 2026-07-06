package io.github.alirezajavan.offlinecap.audio

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PcmResamplerTest {
    @Test
    fun `resample 44100 to 16000 preserves approximate ratio`() {
        val sourceRate = 44_100
        val targetRate = 16_000
        val input = FloatArray(44100) // 1 second
        val resampler = PcmResampler(sourceRate, targetRate)

        val output = resampler.resample(input)

        assertThat(output.size).isEqualTo(16000)
    }

    @Test
    fun `resample identity returns same array`() {
        val input = floatArrayOf(0.1f, 0.2f)
        val resampler = PcmResampler(16_000, 16_000)
        assertThat(resampler.resample(input)).isEqualTo(input)
    }
}
