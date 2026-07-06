package io.github.alirezajavan.offlinecap

import android.content.Context
import io.github.alirezajavan.offlinecap.audio.MediaCodecAudioDecoder
import io.github.alirezajavan.offlinecap.core.engine.AudioDecoder
import io.github.alirezajavan.offlinecap.core.engine.TranscriptionEngine
import io.github.alirezajavan.offlinecap.core.engine.TranslationEngine
import io.github.alirezajavan.offlinecap.core.model.CaptionError
import io.github.alirezajavan.offlinecap.core.model.CaptionEvent
import io.github.alirezajavan.offlinecap.core.model.CaptionRequest
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.WhisperModel
import io.github.alirezajavan.offlinecap.core.pipeline.CaptionPipeline
import io.github.alirezajavan.offlinecap.lingua.MlKitTranslationEngine
import io.github.alirezajavan.offlinecap.scribe.WhisperDecodeOptions
import io.github.alirezajavan.offlinecap.scribe.WhisperTranscriptionEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion

/**
 * Main entry point for the OfflineCap library.
 */
public class OfflineCap internal constructor(
    private val audioDecoder: AudioDecoder,
    private val transcriptionEngine: TranscriptionEngine,
    private val translationEngine: TranslationEngine,
    public val models: ModelManager,
    private val whisperModel: WhisperModel,
) {
    /**
     * Starts the captioning process for the given [request].
     * The returned Flow is cold and work starts when it is collected.
     */
    public fun caption(request: CaptionRequest): Flow<CaptionEvent> =
        flow {
            // 1. Ensure models are ready
            val whisperState = models.state(whisperModel).first()
            if (whisperState !is ModelState.Ready) {
                emit(CaptionEvent.Failed(CaptionError.ModelMissing(whisperModel)))
                return@flow
            }

            transcriptionEngine.load(whisperState.file)

            // 2. Run pipeline
            val pipeline =
                CaptionPipeline(
                    audioDecoder = audioDecoder,
                    transcriptionEngine = transcriptionEngine,
                    translationEngine = translationEngine,
                )

            pipeline.execute(request).collect { emit(it) }
        }.onCompletion {
            transcriptionEngine.close()
            if (translationEngine is AutoCloseable) {
                translationEngine.close()
            }
        }

    public class Builder(
        private val context: Context,
    ) {
        private var whisperModel: WhisperModel = WhisperModel.BASE
        private var decodeOptions: WhisperDecodeOptions = WhisperDecodeOptions()

        public fun transcriptionModel(model: WhisperModel): Builder {
            this.whisperModel = model
            return this
        }

        /**
         * Overrides the whisper decoding speed/accuracy trade-off; see [WhisperDecodeOptions].
         */
        public fun transcriptionOptions(options: WhisperDecodeOptions): Builder {
            this.decodeOptions = options
            return this
        }

        public fun build(): OfflineCap {
            val audioDecoder = MediaCodecAudioDecoder(context)
            val transcriptionEngine = WhisperTranscriptionEngine(decodeOptions)
            val translationEngine = MlKitTranslationEngine()
            val whisperRepository = WhisperModelRepository(context)

            return OfflineCap(
                audioDecoder = audioDecoder,
                transcriptionEngine = transcriptionEngine,
                translationEngine = translationEngine,
                models = ModelManager(whisperRepository, translationEngine),
                whisperModel = whisperModel,
            )
        }
    }
}
