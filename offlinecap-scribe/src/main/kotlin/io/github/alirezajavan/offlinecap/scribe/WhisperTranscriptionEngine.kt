package io.github.alirezajavan.offlinecap.scribe

import android.util.Log
import io.github.alirezajavan.offlinecap.core.engine.TranscriptionEngine
import io.github.alirezajavan.offlinecap.core.engine.TranscriptionEvent
import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.ModelFile
import io.github.alirezajavan.offlinecap.core.model.PcmChunk
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import io.github.alirezajavan.offlinecap.core.model.WordTiming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "OfflineCap.Scribe"
private const val PROGRESS_POLL_INTERVAL_MS = 250L
private const val SAMPLE_RATE_HZ = 16_000

/**
 * Internal interface for the native boundary to allow JVM testing.
 */
internal interface WhisperNative {
    fun initContext(modelPath: String): Long

    fun freeContext(ptr: Long)

    fun transcribeWindow(
        ptr: Long,
        samples: FloatArray,
        offsetMs: Long,
        lang: String?,
        jobId: Int,
        bestOf: Int,
        temperatureFallback: Boolean,
        wordTimestamps: Boolean,
    ): Array<String>?

    fun detectLanguage(
        ptr: Long,
        samples: FloatArray,
    ): String?

    fun cancel(jobId: Int)

    fun getProgress(jobId: Int): Int
}

internal object RealWhisperNative : WhisperNative {
    init {
        System.loadLibrary("offlinecap_scribe")
    }

    override fun initContext(modelPath: String): Long = WhisperJni.initContext(modelPath)

    override fun freeContext(ptr: Long) = WhisperJni.freeContext(ptr)

    override fun transcribeWindow(
        ptr: Long,
        samples: FloatArray,
        offsetMs: Long,
        lang: String?,
        jobId: Int,
        bestOf: Int,
        temperatureFallback: Boolean,
        wordTimestamps: Boolean,
    ) = WhisperJni.transcribeWindow(ptr, samples, offsetMs, lang, jobId, bestOf, temperatureFallback, wordTimestamps)

    override fun detectLanguage(
        ptr: Long,
        samples: FloatArray,
    ) = WhisperJni.detectLanguage(ptr, samples)

    override fun cancel(jobId: Int) = WhisperJni.cancel(jobId)

    override fun getProgress(jobId: Int): Int = WhisperJni.getProgress(jobId)
}

public class WhisperTranscriptionEngine internal constructor(
    private val native: WhisperNative,
    private val options: WhisperDecodeOptions = WhisperDecodeOptions(),
) : TranscriptionEngine {
    public constructor(options: WhisperDecodeOptions = WhisperDecodeOptions()) : this(RealWhisperNative, options)

    private var nativePtr: Long = 0
    private val mutex = Mutex()
    private val jobIdCounter = AtomicInteger(0)
    private val executor = Executors.newSingleThreadExecutor()
    private val nativeDispatcher = executor.asCoroutineDispatcher()

    override suspend fun load(model: ModelFile) {
        withContext(nativeDispatcher) {
            mutex.withLock {
                if (nativePtr != 0L) {
                    native.freeContext(nativePtr)
                }
                nativePtr = native.initContext(model.path)
                if (nativePtr == 0L) {
                    throw IllegalStateException("Failed to initialize whisper context from ${model.path}")
                }
            }
        }
    }

    override fun transcribe(
        audio: Flow<PcmChunk>,
        language: LanguageTag?,
    ): Flow<TranscriptionEvent> =
        channelFlow {
            val jobId = jobIdCounter.getAndIncrement()
            var detectedLanguage: String? = language?.code
            var lastEmittedEndMs = -1L

            audio
                .onCompletion {
                    native.cancel(jobId)
                }.collect { chunk ->
                    val windowDurationMs = chunk.samples.size * 1000L / SAMPLE_RATE_HZ
                    val windowStart = System.currentTimeMillis()

                    val results =
                        withContext(nativeDispatcher) {
                            val ptr = mutex.withLock { nativePtr }
                            if (ptr == 0L) throw IllegalStateException("Whisper context not loaded")

                            // Auto-detect language if not provided
                            if (detectedLanguage == null) {
                                detectedLanguage = native.detectLanguage(ptr, chunk.samples)
                            }

                            coroutineScope {
                                val pollJob =
                                    launch(Dispatchers.Default) {
                                        while (isActive) {
                                            delay(PROGRESS_POLL_INTERVAL_MS)
                                            if (chunk.totalDurationMs > 0) {
                                                val windowProgress = native.getProgress(jobId)
                                                val overall =
                                                    (chunk.startMs + windowDurationMs * windowProgress / 100) /
                                                        chunk.totalDurationMs.toFloat()
                                                send(TranscriptionEvent.Progress(overall.coerceIn(0f, 1f)))
                                            }
                                        }
                                    }
                                try {
                                    native.transcribeWindow(
                                        ptr = ptr,
                                        samples = chunk.samples,
                                        offsetMs = chunk.startMs,
                                        lang = detectedLanguage,
                                        jobId = jobId,
                                        bestOf = options.greedyBestOf,
                                        temperatureFallback = options.temperatureFallback,
                                        wordTimestamps = options.wordTimestamps,
                                    )
                                } finally {
                                    pollJob.cancel()
                                }
                            }
                        }

                    Log.d(
                        TAG,
                        "window offsetMs=${chunk.startMs} elapsedMs=${System.currentTimeMillis() - windowStart}",
                    )

                    if (results != null) {
                        for (cue in parseSegments(results)) {
                            // Overlapping windows (see PcmWindowing) re-transcribe the tail of the
                            // previous window; skip segments fully inside the already-emitted range.
                            if (cue.startMs < lastEmittedEndMs) continue
                            lastEmittedEndMs = maxOf(lastEmittedEndMs, cue.endMs)
                            send(TranscriptionEvent.Segment(cue))
                        }
                    }
                }
        }.flowOn(Dispatchers.Default)

    override fun close() {
        if (nativePtr != 0L) {
            native.freeContext(nativePtr)
            nativePtr = 0L
        }
        executor.shutdown()
    }

    /**
     * Parses the flat, self-describing native result array into cues.
     * See [WhisperJni.transcribeWindow] for the wire format.
     */
    private fun parseSegments(results: Array<String>): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        var i = 0
        while (i + 3 < results.size) {
            val startMs = results[i].toLong()
            val endMs = results[i + 1].toLong()
            val text = results[i + 2]
            val wordCount = results[i + 3].toInt()
            i += 4

            val words = mutableListOf<WordTiming>()
            repeat(wordCount) {
                words.add(
                    WordTiming(
                        text = results[i + 2],
                        startMs = results[i].toLong(),
                        endMs = results[i + 1].toLong(),
                        confidence = results[i + 3].toFloat(),
                    ),
                )
                i += 4
            }

            cues.add(SubtitleCue(index = 0, startMs = startMs, endMs = endMs, text = text, words = words))
        }
        return cues
    }
}
