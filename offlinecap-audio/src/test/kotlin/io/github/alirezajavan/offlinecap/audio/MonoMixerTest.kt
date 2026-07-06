package io.github.alirezajavan.offlinecap.audio

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class MonoMixerTest {
    @Test
    fun `mix stereo to mono averages channels`() {
        val input = floatArrayOf(0.1f, 0.3f, 0.4f, 0.6f) // 2 stereo samples
        val mixer = MonoMixer(2)

        val output = mixer.mix(input)

        assertThat(output).isEqualTo(floatArrayOf(0.2f, 0.5f))
    }

    @Test
    fun `mix mono to mono returns same array`() {
        val input = floatArrayOf(0.1f, 0.2f)
        val mixer = MonoMixer(1)
        assertThat(mixer.mix(input)).isEqualTo(input)
    }
}
