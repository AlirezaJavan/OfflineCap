package io.github.alirezajavan.offlinecap.audio

import io.github.alirezajavan.offlinecap.core.model.PcmChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Utility for windowing a stream of PCM samples.
 */
public object PcmWindowing {
    private const val WINDOW_SIZE_MS = 30_000L
    private const val OVERLAP_MS = 1_000L
    private const val SAMPLE_RATE = 16_000
    private const val WINDOW_SIZE_SAMPLES = (WINDOW_SIZE_MS * SAMPLE_RATE / 1000).toInt()
    private const val OVERLAP_SAMPLES = (OVERLAP_MS * SAMPLE_RATE / 1000).toInt()
    private const val STEP_SAMPLES = WINDOW_SIZE_SAMPLES - OVERLAP_SAMPLES

    /**
     * Accumulates samples from [upstream] and emits fixed-size windows.
     *
     * Uses a primitive [FloatArray] accumulator shifted with `arraycopy`; a boxed
     * list with per-element removal here previously cost tens of seconds per window.
     */
    public fun window(upstream: Flow<PcmChunk>): Flow<PcmChunk> =
        flow {
            var buffer = FloatArray(WINDOW_SIZE_SAMPLES * 2)
            var size = 0
            var windowStartMs = 0L
            var totalDurationMs = 0L

            upstream.collect { chunk ->
                if (size == 0) windowStartMs = chunk.startMs
                totalDurationMs = chunk.totalDurationMs

                if (size + chunk.samples.size > buffer.size) {
                    buffer = buffer.copyOf(maxOf(buffer.size * 2, size + chunk.samples.size))
                }
                chunk.samples.copyInto(buffer, size)
                size += chunk.samples.size

                while (size >= WINDOW_SIZE_SAMPLES) {
                    emit(PcmChunk(buffer.copyOfRange(0, WINDOW_SIZE_SAMPLES), windowStartMs, totalDurationMs))
                    buffer.copyInto(buffer, 0, STEP_SAMPLES, size)
                    size -= STEP_SAMPLES
                    windowStartMs += STEP_SAMPLES * 1000L / SAMPLE_RATE
                }
            }

            // Emit final partial window if any
            if (size > 0) {
                emit(PcmChunk(buffer.copyOfRange(0, size), windowStartMs, totalDurationMs))
            }
        }
}
