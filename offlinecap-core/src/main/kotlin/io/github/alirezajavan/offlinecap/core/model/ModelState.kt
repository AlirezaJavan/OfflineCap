package io.github.alirezajavan.offlinecap.core.model

/**
 * Represents the current state of a model file.
 */
public sealed interface ModelState {
    public data object Missing : ModelState

    public data class Downloading(
        val progress: Float,
    ) : ModelState

    public data class Ready(
        val file: ModelFile,
    ) : ModelState

    public data class Failed(
        val error: ModelError,
    ) : ModelState
}

public sealed interface ModelError {
    public data class NetworkError(
        val cause: Throwable?,
    ) : ModelError

    public data object ChecksumMismatch : ModelError

    public data object StorageError : ModelError
}
