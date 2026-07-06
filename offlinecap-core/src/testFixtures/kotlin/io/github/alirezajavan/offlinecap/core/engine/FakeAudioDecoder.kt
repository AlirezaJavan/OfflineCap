package io.github.alirezajavan.offlinecap.core.engine

import io.github.alirezajavan.offlinecap.core.model.PcmChunk
import io.github.alirezajavan.offlinecap.core.model.PcmSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

public class FakeAudioDecoder(
    public var chunks: List<PcmChunk> = emptyList(),
) : AudioDecoder {
    override fun decode(
        videoUri: String,
        spec: PcmSpec,
    ): Flow<PcmChunk> = chunks.asFlow()
}
