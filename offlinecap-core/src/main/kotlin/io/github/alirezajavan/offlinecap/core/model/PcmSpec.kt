package io.github.alirezajavan.offlinecap.core.model

/**
 * Audio specification for the transcription engine.
 * whisper.cpp requires 16 kHz mono 32-bit float PCM.
 */
public data class PcmSpec(
    public val sampleRate: Int = 16_000,
    public val channels: Int = 1,
    public val encoding: PcmEncoding = PcmEncoding.FLOAT,
)

public enum class PcmEncoding {
    FLOAT,
}
