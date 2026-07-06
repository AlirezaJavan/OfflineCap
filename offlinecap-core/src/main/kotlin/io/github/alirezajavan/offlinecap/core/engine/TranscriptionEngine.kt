package io.github.alirezajavan.offlinecap.core.engine

import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.ModelFile
import io.github.alirezajavan.offlinecap.core.model.PcmChunk
import kotlinx.coroutines.flow.Flow

/**
 * Interface for speech-to-text transcription.
 */
public interface TranscriptionEngine : AutoCloseable {
    /**
     * Loads the given [model] file.
     */
    public suspend fun load(model: ModelFile)

    /**
     * Transcribes the given [audio] stream. If [language] is null, auto-detection is used.
     */
    public fun transcribe(
        audio: Flow<PcmChunk>,
        language: LanguageTag?,
    ): Flow<TranscriptionEvent>
}
