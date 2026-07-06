package io.github.alirezajavan.offlinecap.core.lang

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LanguageTagTest {
    @Test
    fun `parse valid simple code`() {
        val tag = LanguageTag.parse("en")
        assertThat(tag?.code).isEqualTo("en")
    }

    @Test
    fun `parse valid code with case insensitivity`() {
        val tag = LanguageTag.parse("FA")
        assertThat(tag?.code).isEqualTo("fa")
    }

    @ParameterizedTest
    @ValueSource(strings = ["en-US", "en_US", "en-GB"])
    fun `parse BCP47 or similar formats`(raw: String) {
        val tag = LanguageTag.parse(raw)
        assertThat(tag?.code).isEqualTo("en")
    }

    @Test
    fun `parse returns null for blank`() {
        assertThat(LanguageTag.parse("")).isNull()
        assertThat(LanguageTag.parse("   ")).isNull()
    }

    @Test
    fun `parse returns null for invalid codes`() {
        assertThat(LanguageTag.parse("12")).isNull()
        assertThat(LanguageTag.parse("a")).isNull()
    }
}
