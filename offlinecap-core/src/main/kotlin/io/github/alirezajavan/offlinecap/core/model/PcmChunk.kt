package io.github.alirezajavan.offlinecap.core.model

/**
 * A chunk of PCM audio data.
 *
 * @property totalDurationMs total duration of the source audio in milliseconds,
 * or 0 if unknown. Carried alongside [startMs] so downstream stages (decode
 * progress, overall transcription progress) can compute a fraction complete
 * without a separate side-channel.
 */
public data class PcmChunk(
    public val samples: FloatArray,
    public val startMs: Long,
    public val totalDurationMs: Long = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PcmChunk) return false

        if (!samples.contentEquals(other.samples)) return false
        if (startMs != other.startMs) return false
        if (totalDurationMs != other.totalDurationMs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + startMs.hashCode()
        result = 31 * result + totalDurationMs.hashCode()
        return result
    }
}
