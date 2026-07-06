package io.github.alirezajavan.offlinecap.core.engine

import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.WhisperModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

public class FakeModelRepository : ModelRepository {
    override fun state(model: WhisperModel): Flow<ModelState> = emptyFlow()

    override fun download(model: WhisperModel): Flow<ModelState> = emptyFlow()

    override suspend fun delete(model: WhisperModel) {}
}
