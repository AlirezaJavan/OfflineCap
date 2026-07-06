package io.github.alirezajavan.offlinecap.core.engine

import io.github.alirezajavan.offlinecap.core.model.PcmChunk
import io.github.alirezajavan.offlinecap.core.model.PcmSpec
import kotlinx.coroutines.flow.Flow

/**
 * Interface for extracting audio from video files.
 */
public interface AudioDecoder {
    /**
     * Decodes the audio from [videoUri] to a stream of [PcmChunk]s according to [spec].
     */
    public fun decode(
        videoUri: String,
        spec: PcmSpec,
    ): Flow<PcmChunk>
}
