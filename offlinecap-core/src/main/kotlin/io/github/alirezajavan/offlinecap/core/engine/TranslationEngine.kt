package io.github.alirezajavan.offlinecap.core.engine

import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.ModelState
import kotlinx.coroutines.flow.Flow

/**
 * Interface for on-device translation.
 */
public interface TranslationEngine {
    /**
     * Ensures the models for the given language pair are available.
     */
    public suspend fun ensureModel(
        source: LanguageTag,
        target: LanguageTag,
    ): Flow<ModelState>

    /**
     * Translates the given [text] from [source] to [target] language.
     */
    public suspend fun translate(
        text: String,
        source: LanguageTag,
        target: LanguageTag,
    ): String
}
