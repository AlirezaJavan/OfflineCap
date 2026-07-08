package io.github.alirezajavan.offlinecap.scribe

/**
 * Tuning knobs for whisper.cpp decoding, trading accuracy for speed.
 *
 * The defaults favour speed (single greedy candidate, no temperature-fallback
 * retries), which typically cuts inference time 20-40% versus whisper.cpp
 * defaults at a small accuracy cost on difficult or noisy audio.
 */
public data class WhisperDecodeOptions(
    /**
     * Number of candidate sequences sampled during greedy decoding; the best one
     * is kept. `1` is fastest; whisper.cpp's own default is `5`.
     */
    public val greedyBestOf: Int = 1,
    /**
     * Whether whisper.cpp may re-decode a window at increasing temperatures when
     * it judges the first pass unreliable. Disabling avoids costly retry passes.
     */
    public val temperatureFallback: Boolean = false,
    /**
     * Whether to request per-word timestamps from whisper.cpp and populate
     * [io.github.alirezajavan.offlinecap.core.model.SubtitleCue.words]. Disabled by
     * default since token-level timestamping adds decoding overhead.
     */
    public val wordTimestamps: Boolean = false,
)
