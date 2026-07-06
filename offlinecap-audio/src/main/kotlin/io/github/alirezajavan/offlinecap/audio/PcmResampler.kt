package io.github.alirezajavan.offlinecap.audio

/**
 * Pure Kotlin linear resampler.
 */
public class PcmResampler(
    private val sourceRate: Int,
    private val targetRate: Int = 16_000,
) {
    /**
     * Resamples the given [input] samples.
     */
    public fun resample(input: FloatArray): FloatArray {
        if (sourceRate == targetRate) return input

        val ratio = sourceRate.toDouble() / targetRate.toDouble()
        val targetSize = (input.size / ratio).toInt()
        val output = FloatArray(targetSize)

        for (i in 0 until targetSize) {
            val sourceIndex = i * ratio
            val indexInt = sourceIndex.toInt()
            val fraction = sourceIndex - indexInt

            if (indexInt + 1 < input.size) {
                output[i] = input[indexInt] * (1.0f - fraction.toFloat()) + input[indexInt + 1] * fraction.toFloat()
            } else {
                output[i] = input[indexInt]
            }
        }
        return output
    }
}
