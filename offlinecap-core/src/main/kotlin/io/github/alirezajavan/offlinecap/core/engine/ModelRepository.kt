package io.github.alirezajavan.offlinecap.core.engine

import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.WhisperModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Whisper model files.
 */
public interface ModelRepository {
    public fun state(model: WhisperModel): Flow<ModelState>

    public fun download(model: WhisperModel): Flow<ModelState>

    public suspend fun delete(model: WhisperModel)
}
