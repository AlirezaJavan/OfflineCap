package io.github.alirezajavan.offlinecap.subtitle

import io.github.alirezajavan.offlinecap.core.model.Transcript
import io.github.alirezajavan.offlinecap.core.subtitle.SubtitleWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON subtitle/transcript writer.
 */
internal class JsonTranscriptWriter : SubtitleWriter {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    override fun write(transcript: Transcript): String = json.encodeToString(transcript)
}
