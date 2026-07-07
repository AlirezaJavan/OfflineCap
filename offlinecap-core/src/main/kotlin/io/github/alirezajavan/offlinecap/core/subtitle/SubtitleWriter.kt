package io.github.alirezajavan.offlinecap.core.subtitle

import io.github.alirezajavan.offlinecap.core.model.Transcript

/**
 * Interface for writing a [Transcript] to a subtitle string.
 */
public interface SubtitleWriter {
    public fun write(transcript: Transcript): String
}
