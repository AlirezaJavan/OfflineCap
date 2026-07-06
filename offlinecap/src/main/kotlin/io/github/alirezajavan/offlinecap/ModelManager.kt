package io.github.alirezajavan.offlinecap

import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.WhisperModel
import io.github.alirezajavan.offlinecap.lingua.MlKitTranslationEngine
import kotlinx.coroutines.flow.Flow

/**
 * Unified manager for Whisper and ML Kit models.
 */
public class ModelManager(
    private val whisperRepository: WhisperModelRepository,
    private val translationEngine: MlKitTranslationEngine,
) {
    /**
     * Returns a flow of the current state of a Whisper [model].
     */
    public fun state(model: WhisperModel): Flow<ModelState> = whisperRepository.state(model)

    /**
     * Starts downloading a Whisper [model].
     */
    public fun download(model: WhisperModel): Flow<ModelState> = whisperRepository.download(model)

    /**
     * Deletes a Whisper [model] from local storage.
     */
    public suspend fun delete(model: WhisperModel): Unit = whisperRepository.delete(model)

    /**
     * Ensures translation models for the given language pair are downloaded.
     */
    public suspend fun ensureTranslationModel(
        source: LanguageTag,
        target: LanguageTag,
    ): Flow<ModelState> = translationEngine.ensureModel(source, target)
}
