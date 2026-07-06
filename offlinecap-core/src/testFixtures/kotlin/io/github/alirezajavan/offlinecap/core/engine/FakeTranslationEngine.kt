package io.github.alirezajavan.offlinecap.core.engine

import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.ModelState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

public class FakeTranslationEngine : TranslationEngine {
    override suspend fun ensureModel(
        source: LanguageTag,
        target: LanguageTag,
    ): Flow<ModelState> = emptyFlow()

    override suspend fun translate(
        text: String,
        source: LanguageTag,
        target: LanguageTag,
    ): String = "Translated: $text"
}
