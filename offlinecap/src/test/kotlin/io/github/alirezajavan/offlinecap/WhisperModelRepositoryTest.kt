package io.github.alirezajavan.offlinecap

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.alirezajavan.offlinecap.core.model.ModelError
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.WhisperModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class WhisperModelRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: WhisperModelRepository
    private lateinit var context: Context

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()

        context = mockk()
        every { context.filesDir } returns tempDir

        // These flow tests exercise download/progress orchestration, not checksum matching
        // (WhisperModel's real sha256 values aren't known here), so the verifier is stubbed.
        repository =
            WhisperModelRepository(
                context,
                baseUrlOverride = server.url("").toString(),
                checksumVerifier = { _, _ -> true },
            )
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `state returns Missing when file does not exist`() =
        runTest {
            repository.state(WhisperModel.TINY).test {
                assertThat(awaitItem()).isEqualTo(ModelState.Missing)
                awaitComplete()
            }
        }

    @Test
    fun `download emits progress and ready`() =
        runTest {
            val content = "fake model content"
            server.enqueue(MockResponse().setBody(content))

            repository.download(WhisperModel.TINY).test {
                // Initial progress (might be multiple events depending on buffer size)
                val first = awaitItem()
                assertThat(first).isInstanceOf(ModelState.Downloading::class.java)

                // Final state should be Ready
                var lastState: ModelState = first
                while (lastState !is ModelState.Ready) {
                    lastState = awaitItem()
                }

                assertThat(lastState).isInstanceOf(ModelState.Ready::class.java)
                val file = (lastState as ModelState.Ready).file
                assertThat(File(file.path).readText()).isEqualTo(content)

                awaitComplete()
            }
        }

    @Test
    fun `defaultVerifyChecksum matches a known SHA-256 digest`() {
        val file = File(tempDir, "known-content.bin").apply { writeText("abc") }

        assertThat(
            defaultVerifyChecksum(file, "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),
        ).isTrue()
    }

    @Test
    fun `defaultVerifyChecksum rejects a mismatched digest`() {
        val file = File(tempDir, "known-content.bin").apply { writeText("abc") }

        assertThat(defaultVerifyChecksum(file, "0".repeat(64))).isFalse()
    }

    @Test
    fun `download emits Failed on checksum mismatch`() =
        runTest {
            val repositoryWithRealVerifier =
                WhisperModelRepository(context, baseUrlOverride = server.url("").toString())
            server.enqueue(MockResponse().setBody("fake model content"))

            repositoryWithRealVerifier.download(WhisperModel.TINY).test {
                var state = awaitItem()
                while (state is ModelState.Downloading) {
                    state = awaitItem()
                }

                assertThat(state).isInstanceOf(ModelState.Failed::class.java)
                assertThat((state as ModelState.Failed).error).isInstanceOf(ModelError.ChecksumMismatch::class.java)
                awaitComplete()
            }
        }

    @Test
    fun `download emits Failed on network error`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(404))

            repository.download(WhisperModel.TINY).test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(ModelState.Failed::class.java)
                val error = (state as ModelState.Failed).error
                assertThat(error).isInstanceOf(ModelError.NetworkError::class.java)
                awaitComplete()
            }
        }
}
