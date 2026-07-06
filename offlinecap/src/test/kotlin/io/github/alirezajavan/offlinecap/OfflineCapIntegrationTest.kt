package io.github.alirezajavan.offlinecap

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.alirezajavan.offlinecap.core.engine.FakeAudioDecoder
import io.github.alirezajavan.offlinecap.core.engine.FakeTranscriptionEngine
import io.github.alirezajavan.offlinecap.core.engine.FakeTranslationEngine
import io.github.alirezajavan.offlinecap.core.model.CaptionEvent
import io.github.alirezajavan.offlinecap.core.model.CaptionRequest
import io.github.alirezajavan.offlinecap.core.model.ModelFile
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import io.github.alirezajavan.offlinecap.core.model.WhisperModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class OfflineCapIntegrationTest {
    @Test
    fun `full pipeline flow works with fakes`() =
        runTest {
            val audioDecoder = FakeAudioDecoder()
            val transcriptionEngine =
                FakeTranscriptionEngine(
                    results = listOf(SubtitleCue(0, 0, 1000, "Hello")),
                )
            val translationEngine = FakeTranslationEngine()
            val models = mockk<ModelManager>()

            every { models.state(WhisperModel.BASE) } returns flowOf(ModelState.Ready(ModelFile("fake", 0)))

            val offlineCap =
                OfflineCap(
                    audioDecoder = audioDecoder,
                    transcriptionEngine = transcriptionEngine,
                    translationEngine = translationEngine,
                    models = models,
                    whisperModel = WhisperModel.BASE,
                )

            val request =
                CaptionRequest(
                    videoUri = "fake.mp4",
                    sourceLanguage = null,
                    targetLanguage = null,
                )

            offlineCap.caption(request).test {
                // ExtractingAudio(0.0) is emitted by pipeline
                assertThat(awaitItem()).isInstanceOf(CaptionEvent.ExtractingAudio::class.java)

                // Transcribing(1.0, cue) is emitted by pipeline for each segment from fake
                assertThat(awaitItem()).isInstanceOf(CaptionEvent.Transcribing::class.java)

                // Completed is emitted at the end
                assertThat(awaitItem()).isInstanceOf(CaptionEvent.Completed::class.java)

                awaitComplete()
            }
        }
}
