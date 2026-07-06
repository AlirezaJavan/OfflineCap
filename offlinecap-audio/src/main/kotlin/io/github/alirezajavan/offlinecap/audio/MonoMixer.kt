package io.github.alirezajavan.offlinecap.audio

/**
 * Mixes multi-channel audio to mono.
 */
public class MonoMixer(
    private val channelCount: Int,
) {
    /**
     * Mixes the given [input] samples (interleaved) to mono.
     */
    public fun mix(input: FloatArray): FloatArray {
        if (channelCount == 1) return input

        val targetSize = input.size / channelCount
        val output = FloatArray(targetSize)

        for (i in 0 until targetSize) {
            var sum = 0f
            for (c in 0 until channelCount) {
                sum += input[i * channelCount + c]
            }
            output[i] = sum / channelCount
        }
        return output
    }
}
