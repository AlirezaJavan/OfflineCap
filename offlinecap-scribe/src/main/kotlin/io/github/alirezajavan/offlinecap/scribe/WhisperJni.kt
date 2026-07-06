package io.github.alirezajavan.offlinecap.scribe

/**
 * Low-level JNI bridge for whisper.cpp.
 */
internal object WhisperJni {
    init {
        System.loadLibrary("offlinecap_scribe")
    }

    external fun initContext(modelPath: String): Long

    external fun freeContext(ptr: Long)

    /**
     * Transcribes a window of PCM samples.
     * Returns a flat array of strings: [startMs, endMs, text, startMs, endMs, text, ...]
     */
    external fun transcribeWindow(
        ptr: Long,
        samples: FloatArray,
        offsetMs: Long,
        lang: String?,
        jobId: Int,
        bestOf: Int,
        temperatureFallback: Boolean,
    ): Array<String>?

    external fun detectLanguage(
        ptr: Long,
        samples: FloatArray,
    ): String?

    external fun cancel(jobId: Int)

    /**
     * Returns the whisper.cpp internal progress (0-100) for the currently
     * running window of the given job, as last reported by its progress_callback.
     */
    external fun getProgress(jobId: Int): Int
}
