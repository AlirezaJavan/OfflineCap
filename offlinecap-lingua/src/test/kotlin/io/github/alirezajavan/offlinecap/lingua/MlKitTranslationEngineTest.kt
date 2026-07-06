package io.github.alirezajavan.offlinecap.lingua

import app.cash.turbine.test
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MlKitTranslationEngineTest {
    private lateinit var modelManager: RemoteModelManager
    private lateinit var engine: MlKitTranslationEngine

    @BeforeEach
    fun setUp() {
        modelManager = mockk()
        mockkStatic(RemoteModelManager::class)
        every { RemoteModelManager.getInstance() } returns modelManager

        engine =
            object : MlKitTranslationEngine() {
                override fun createRemoteModel(code: String): TranslateRemoteModel = mockk()

                override fun createTranslator(options: TranslatorOptions): Translator =
                    mockk {
                        every { downloadModelIfNeeded() } returns Tasks.forResult(null)
                        every { close() } returns Unit
                    }
            }
    }

    @AfterEach
    fun tearDown() {
        engine.close()
        unmockkAll()
    }

    @Test
    fun `ensureModel emits ready if already downloaded`() =
        runTest {
            every { modelManager.isModelDownloaded(any()) } returns Tasks.forResult(true)

            engine.ensureModel(LanguageTag("en"), LanguageTag("fa")).test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(ModelState.Ready::class.java)
                awaitComplete()
            }
        }

    @Test
    fun `translate calls mlkit translator`() =
        runTest {
            // Re-create engine to customize the mock translator
            val mockTranslator = mockk<Translator>()
            every { mockTranslator.downloadModelIfNeeded() } returns Tasks.forResult(null)
            every { mockTranslator.translate("Hello") } returns Tasks.forResult("سلام")
            every { mockTranslator.close() } returns Unit

            val testEngine =
                object : MlKitTranslationEngine() {
                    override fun createTranslator(options: TranslatorOptions): Translator = mockTranslator
                }

            val result = testEngine.translate("Hello", LanguageTag("en"), LanguageTag("fa"))

            assertThat(result).isEqualTo("سلام")
        }
}
