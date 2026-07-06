package io.github.alirezajavan.offlinecap.scribe

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.alirezajavan.offlinecap.core.engine.TranscriptionEvent
import io.github.alirezajavan.offlinecap.core.model.ModelFile
import io.github.alirezajavan.offlinecap.core.model.PcmChunk
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
                native.transcribeWindow(123L, any(), any(), any(), any(), any(), any())
            } returns arrayOf("0", "1000", "Hello")
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
                awaitComplete()
            }
        }
}
