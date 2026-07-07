package io.github.alirezajavan.offlinecap.core.pipeline

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.alirezajavan.offlinecap.core.engine.FakeAudioDecoder
import io.github.alirezajavan.offlinecap.core.engine.FakeSubtitleFormatter
import io.github.alirezajavan.offlinecap.core.engine.FakeTranscriptionEngine
import io.github.alirezajavan.offlinecap.core.engine.FakeTranslationEngine
import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.CaptionEvent
import io.github.alirezajavan.offlinecap.core.model.CaptionRequest
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CaptionPipelineTest {
    @Test
    fun `pipeline happy path emits completed event`() =
        runTest {
            val audioDecoder = FakeAudioDecoder()
            val transcriptionEngine =
                FakeTranscriptionEngine(
                    results = listOf(SubtitleCue(0, 0, 1000, "Hello world")),
                )
            val translationEngine = FakeTranslationEngine()

            val pipeline =
                CaptionPipeline(audioDecoder, transcriptionEngine, translationEngine, FakeSubtitleFormatter())
            val request =
                CaptionRequest(
                    videoUri = "content://media/1",
                    sourceLanguage = LanguageTag("en"),
                    targetLanguage = LanguageTag("fa"),
                )

            pipeline.execute(request).test {
                // Wait for completion
                var lastEvent: CaptionEvent? = null
                while (true) {
                    val event = awaitItem()
                    lastEvent = event
                    if (event is CaptionEvent.Completed) break
                }

                val result = (lastEvent as CaptionEvent.Completed).result
                assertThat(result.transcript.cues).hasSize(1)
                assertThat(result.transcript.cues[0].text).isEqualTo("Hello world")
                assertThat(
                    result.translatedTranscript
                        ?.cues
                        ?.get(0)
                        ?.text,
                ).contains("Translated:")
                cancelAndIgnoreRemainingEvents()
            }
        }
}
