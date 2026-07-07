package io.github.alirezajavan.offlinecap.transcribe

import io.github.alirezajavan.offlinecap.core.engine.TranscriptionEngine
import io.github.alirezajavan.offlinecap.core.model.ModelFile
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.WhisperModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AudioTranscriberTest {
    @Test
    fun `loadModel throws when model is not downloaded`() =
        runTest {
            val engine = mockk<TranscriptionEngine>()
            val models = mockk<WhisperModelRepository>()
            every { models.state(WhisperModel.BASE) } returns flowOf(ModelState.Missing)

            val transcriber = AudioTranscriber(engine, models)

            assertThrows<IllegalStateException> {
                transcriber.loadModel(WhisperModel.BASE)
            }
        }

    @Test
    fun `loadModel loads the underlying engine once the model is ready`() =
        runTest {
            val engine = mockk<TranscriptionEngine>()
            val models = mockk<WhisperModelRepository>()
            val modelFile = ModelFile("fake/path", 123)
            every { models.state(WhisperModel.BASE) } returns flowOf(ModelState.Ready(modelFile))
            coEvery { engine.load(modelFile) } returns Unit

            val transcriber = AudioTranscriber(engine, models)
            transcriber.loadModel(WhisperModel.BASE)

            coVerify { engine.load(modelFile) }
        }

    @Test
    fun `transcribe and close delegate to the underlying engine`() {
        val engine = mockk<TranscriptionEngine>()
        val models = mockk<WhisperModelRepository>()
        every { engine.transcribe(any(), any()) } returns emptyFlow()
        every { engine.close() } returns Unit

        val transcriber = AudioTranscriber(engine, models)
        transcriber.transcribe(emptyFlow(), null)
        transcriber.close()

        verify { engine.transcribe(any(), any()) }
        verify { engine.close() }
    }
}
