package io.github.alirezajavan.offlinecap.core.model

import io.github.alirezajavan.offlinecap.core.lang.LanguageTag

/**
 * Request parameters for the captioning pipeline.
 */
public data class CaptionRequest(
    public val videoUri: String,
    public val sourceLanguage: LanguageTag? = null,
    public val targetLanguage: LanguageTag? = null,
    public val format: SubtitleFormat = SubtitleFormat.SRT,
)
