package io.github.alirezajavan.offlinecap.lingua

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import io.github.alirezajavan.offlinecap.core.engine.TranslationEngine
import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.ModelError
import io.github.alirezajavan.offlinecap.core.model.ModelFile
import io.github.alirezajavan.offlinecap.core.model.ModelState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

/**
 * On-device translation using Google ML Kit.
 */
public open class MlKitTranslationEngine :
    TranslationEngine,
    AutoCloseable {
    private val modelManager = RemoteModelManager.getInstance()
    private val translators = ConcurrentHashMap<Pair<LanguageTag, LanguageTag>, Translator>()

    override suspend fun ensureModel(
        source: LanguageTag,
        target: LanguageTag,
    ): Flow<ModelState> =
        flow {
            val sourceModel = createRemoteModel(source.toMlKitCode())
            val targetModel = createRemoteModel(target.toMlKitCode())

            suspend fun ensure(model: TranslateRemoteModel) {
                if (!modelManager.isModelDownloaded(model).await()) {
                    emit(ModelState.Downloading(0f))
                    modelManager.download(model, DownloadConditions.Builder().build()).await()
                    emit(ModelState.Downloading(1f))
                }
            }

            try {
                ensure(sourceModel)
                ensure(targetModel)
                // ML Kit models are managed by the SDK, we don't have a direct file path.
                emit(ModelState.Ready(ModelFile("mlkit://${source.code}-${target.code}", 0)))
            } catch (e: Exception) {
                emit(ModelState.Failed(ModelError.NetworkError(e)))
            }
        }

    override suspend fun translate(
        text: String,
        source: LanguageTag,
        target: LanguageTag,
    ): String {
        val translator = getTranslator(source, target)
        return translator.translate(text).await()
    }

    internal open fun createRemoteModel(code: String): TranslateRemoteModel = TranslateRemoteModel.Builder(code).build()

    internal open fun createTranslator(options: TranslatorOptions): Translator = Translation.getClient(options)

    private suspend fun getTranslator(
        source: LanguageTag,
        target: LanguageTag,
    ): Translator {
        val key = source to target
        val existing = translators[key]
        if (existing != null) return existing

        val options =
            TranslatorOptions
                .Builder()
                .setSourceLanguage(source.toMlKitCode())
                .setTargetLanguage(target.toMlKitCode())
                .build()
        val translator = createTranslator(options)
        // Ensure model is ready (should have been handled by ensureModel, but for safety)
        translator.downloadModelIfNeeded().await()
        translators[key] = translator
        return translator
    }

    override fun close() {
        translators.values.forEach { it.close() }
        translators.clear()
    }

    private fun LanguageTag.toMlKitCode(): String = TranslateLanguage.fromLanguageTag(code) ?: code
}
