package io.github.alirezajavan.offlinecap.transcribe

import android.content.Context
import io.github.alirezajavan.offlinecap.core.engine.TranscriptionEngine
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.WhisperModel
import io.github.alirezajavan.offlinecap.scribe.WhisperDecodeOptions
import io.github.alirezajavan.offlinecap.scribe.WhisperTranscriptionEngine
import kotlinx.coroutines.flow.first

/**
 * Audio-to-transcript facade: a [TranscriptionEngine] backed by whisper.cpp, paired with
 * [models] for downloading and inspecting the Whisper model catalog. Usable standalone
 * (drop the PCM stream from any [io.github.alirezajavan.offlinecap.core.engine.AudioDecoder] straight in)
 * or as the `transcriptionEngine` plugged into `CaptionPipeline`.
 */
public class AudioTranscriber internal constructor(
    private val engine: TranscriptionEngine,
    public val models: WhisperModelRepository,
) : TranscriptionEngine by engine {
    /**
     * Ensures [model] is downloaded and loads it into the underlying engine.
     */
    public suspend fun loadModel(model: WhisperModel) {
        val state = models.state(model).first()
        check(state is ModelState.Ready) { "Model $model is not downloaded; call models.download(model) first." }
        load(state.file)
    }

    public class Builder(
        private val context: Context,
    ) {
        private var decodeOptions: WhisperDecodeOptions = WhisperDecodeOptions()

        /**
         * Overrides the whisper decoding speed/accuracy trade-off; see [WhisperDecodeOptions].
         */
        public fun transcriptionOptions(options: WhisperDecodeOptions): Builder = apply { decodeOptions = options }

        public fun build(): AudioTranscriber =
            AudioTranscriber(WhisperTranscriptionEngine(decodeOptions), WhisperModelRepository(context))
    }
}
