package io.github.alirezajavan.offlinecap.core.model

/**
 * Supported Whisper models with their metadata.
 *
 * `_Q5_0`/`_Q5_1`/`_Q8_0` variants are quantized ggml models: same architecture, smaller weights,
 * meaningfully faster CPU inference (roughly 2-3x over F16) at a small accuracy cost.
 * Prefer them over the F16 variants unless maximum accuracy is required.
 *
 * `sizeBytes` and `sha256` were computed directly from the published `ggml-*.bin` files
 * (huggingface.co/ggerganov/whisper.cpp) rather than transcribed from a third party.
 */
public enum class WhisperModel(
    public val modelName: String,
    public val downloadUrl: String,
    public val sizeBytes: Long,
    public val sha256: String,
) {
    TINY(
        modelName = "tiny",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
        sizeBytes = 77_691_713,
        sha256 = "be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21",
    ),
    TINY_Q5_1(
        modelName = "tiny-q5_1",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin",
        sizeBytes = 32_152_673,
        sha256 = "818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7",
    ),
    TINY_Q8_0(
        modelName = "tiny-q8_0",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q8_0.bin",
        sizeBytes = 43_537_433,
        sha256 = "c2085835d3f50733e2ff6e4b41ae8a2b8d8110461e18821b09a15c40c42d1cca",
    ),
    BASE(
        modelName = "base",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
        sizeBytes = 147_951_465,
        sha256 = "60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe",
    ),
    BASE_Q5_1(
        modelName = "base-q5_1",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin",
        sizeBytes = 59_707_625,
        sha256 = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
    ),
    BASE_Q8_0(
        modelName = "base-q8_0",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q8_0.bin",
        sizeBytes = 81_768_585,
        sha256 = "c577b9a86e7e048a0b7eada054f4dd79a56bbfa911fbdacf900ac5b567cbb7d9",
    ),
    SMALL(
        modelName = "small",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
        sizeBytes = 487_601_967,
        sha256 = "1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b",
    ),
    SMALL_Q5_1(
        modelName = "small-q5_1",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin",
        sizeBytes = 190_085_487,
        sha256 = "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb",
    ),
    SMALL_Q8_0(
        modelName = "small-q8_0",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q8_0.bin",
        sizeBytes = 264_464_607,
        sha256 = "49c8fb02b65e6049d5fa6c04f81f53b867b5ec9540406812c643f177317f779f",
    ),
    MEDIUM(
        modelName = "medium",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin",
        sizeBytes = 1_533_763_059,
        sha256 = "6c14d5adee5f86394037b4e4e8b59f1673b6cee10e3cf0b11bbdbee79c156208",
    ),
    MEDIUM_Q5_0(
        modelName = "medium-q5_0",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium-q5_0.bin",
        sizeBytes = 539_212_467,
        sha256 = "19fea4b380c3a618ec4723c3eef2eb785ffba0d0538cf43f8f235e7b3b34220f",
    ),
    MEDIUM_Q8_0(
        modelName = "medium-q8_0",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium-q8_0.bin",
        sizeBytes = 823_369_779,
        sha256 = "42a1ffcbe4167d224232443396968db4d02d4e8e87e213d3ee2e03095dea6502",
    ),
    LARGE_V3_TURBO(
        modelName = "large-v3-turbo",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo.bin",
        sizeBytes = 1_624_555_275,
        sha256 = "1fc70f774d38eb169993ac391eea357ef47c88757ef72ee5943879b7e8e2bc69",
    ),
    LARGE_V3_TURBO_Q5_0(
        modelName = "large-v3-turbo-q5_0",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo-q5_0.bin",
        sizeBytes = 574_041_195,
        sha256 = "394221709cd5ad1f40c46e6031ca61bce88931e6e088c188294c6d5a55ffa7e2",
    ),
    LARGE_V3_TURBO_Q8_0(
        modelName = "large-v3-turbo-q8_0",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo-q8_0.bin",
        sizeBytes = 874_188_075,
        sha256 = "317eb69c11673c9de1e1f0d459b253999804ec71ac4c23c17ecf5fbe24e259a1",
    ),
}

public data class ModelFile(
    public val path: String,
    public val sizeBytes: Long,
)
