package io.github.alirezajavan.offlinecap.core.model

import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import kotlinx.serialization.Serializable

/**
 * A collection of subtitle cues representing a full transcription in a specific language.
 */
@Serializable
public data class Transcript(
    public val cues: List<SubtitleCue>,
    public val language: LanguageTag,
)
