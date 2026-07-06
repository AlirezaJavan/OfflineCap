package io.github.alirezajavan.offlinecap.core.engine

import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.ModelFile
import io.github.alirezajavan.offlinecap.core.model.PcmChunk
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

public class FakeTranscriptionEngine(
    public var results: List<SubtitleCue> = emptyList(),
) : TranscriptionEngine {
    override suspend fun load(model: ModelFile) {}

    override fun transcribe(
        audio: Flow<PcmChunk>,
        language: LanguageTag?,
    ): Flow<TranscriptionEvent> =
        flow {
            results.forEach { emit(TranscriptionEvent.Segment(it)) }
        }

    override fun close() {}
}
