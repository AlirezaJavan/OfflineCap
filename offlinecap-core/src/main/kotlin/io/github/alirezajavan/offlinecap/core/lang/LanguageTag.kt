package io.github.alirezajavan.offlinecap.core.lang

import kotlinx.serialization.Serializable

/**
 * Value class representing an ISO 639-1 language tag (e.g., "en", "fa").
 */
@JvmInline
@Serializable
public value class LanguageTag(
    public val code: String,
) {
    init {
        require(code.isNotBlank()) { "Language code cannot be blank" }
    }

    override fun toString(): String = code

    public companion object {
        /**
         * Parses a raw string into a [LanguageTag].
         * Normalizes common formats like "en-US", "eng" to 639-1 where possible.
         */
        public fun parse(raw: String): LanguageTag? {
            if (raw.isBlank()) return null

            // Simple normalization for now; Phase 2.3 mentions mapping table for ~99 languages.
            val normalized = raw.trim().lowercase().takeWhile { it.isLetter() }
            if (normalized.length < 2) return null

            // TODO: Add full 639-2 to 639-1 mapping table as required by §2.3
            return LanguageTag(normalized.take(2))
        }
    }
}
