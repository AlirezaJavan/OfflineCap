package io.github.alirezajavan.offlinecap.scribe

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.alirezajavan.offlinecap.core.engine.TranscriptionEvent
import io.github.alirezajavan.offlinecap.core.model.ModelFile
import io.github.alirezajavan.offlinecap.core.model.PcmChunk
import io.github.alirezajavan.offlinecap.core.model.WordTiming
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class WhisperTranscriptionEngineTest {
    @Test
    fun `transcribe emits segments from native`() =
        runTest {
            val native = mockk<WhisperNative>()
            every { native.initContext(any()) } returns 123L
            every { native.freeContext(123L) } returns Unit
            every { native.detectLanguage(123L, any()) } returns "en"
            every {
                native.transcribeWindow(123L, any(), any(), any(), any(), any(), any(), any())
            } returns arrayOf("0", "1000", "Hello", "0")
            every { native.cancel(any()) } returns Unit
            every { native.getProgress(any()) } returns 100

            val engine = WhisperTranscriptionEngine(native)
            engine.load(ModelFile("path/to/model", 100))

            val audio = flowOf(PcmChunk(floatArrayOf(0f), 0))

            engine.transcribe(audio, null).test {
                val event = awaitItem()
                assertThat(event).isInstanceOf(TranscriptionEvent.Segment::class.java)
                val segment = (event as TranscriptionEvent.Segment).cue
                assertThat(segment.text).isEqualTo("Hello")
                assertThat(segment.startMs).isEqualTo(0)
                assertThat(segment.endMs).isEqualTo(1000)
                assertThat(segment.words).isEmpty()
                awaitComplete()
            }
        }

    @Test
    fun `transcribe maps token stream to word timings when enabled`() =
        runTest {
            val native = mockk<WhisperNative>()
            every { native.initContext(any()) } returns 123L
            every { native.freeContext(123L) } returns Unit
            every { native.detectLanguage(123L, any()) } returns "en"
            every {
                native.transcribeWindow(123L, any(), any(), any(), any(), any(), any(), any())
            } returns
                arrayOf("0", "1000", "Hello world", "2", "0", "400", "Hello", "0.9", "400", "1000", "world", "0.8")
            every { native.cancel(any()) } returns Unit
            every { native.getProgress(any()) } returns 100

            val options = WhisperDecodeOptions(wordTimestamps = true)
            val engine = WhisperTranscriptionEngine(native, options)
            engine.load(ModelFile("path/to/model", 100))

            val audio = flowOf(PcmChunk(floatArrayOf(0f), 0))

            engine.transcribe(audio, null).test {
                val event = awaitItem()
                val segment = (event as TranscriptionEvent.Segment).cue
                assertThat(segment.words)
                    .containsExactly(
                        WordTiming(text = "Hello", startMs = 0, endMs = 400, confidence = 0.9f),
                        WordTiming(text = "world", startMs = 400, endMs = 1000, confidence = 0.8f),
                    ).inOrder()
                awaitComplete()
            }
        }
}
