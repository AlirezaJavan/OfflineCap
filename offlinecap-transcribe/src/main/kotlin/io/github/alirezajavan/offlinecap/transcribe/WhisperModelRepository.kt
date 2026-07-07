package io.github.alirezajavan.offlinecap.transcribe

import android.content.Context
import io.github.alirezajavan.offlinecap.core.engine.ModelRepository
import io.github.alirezajavan.offlinecap.core.model.ModelError
import io.github.alirezajavan.offlinecap.core.model.ModelFile
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.WhisperModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Computes the lowercase hex SHA-256 digest of [file]'s contents.
 */
internal fun sha256Hex(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

/**
 * Default [WhisperModelRepository] checksum verifier: true if [file] exists, is non-empty,
 * and its SHA-256 digest matches [expectedSha256] (case-insensitive).
 */
internal fun defaultVerifyChecksum(
    file: File,
    expectedSha256: String,
): Boolean = file.exists() && file.length() > 0 && sha256Hex(file).equals(expectedSha256, ignoreCase = true)

/**
 * Android implementation of [ModelRepository] for Whisper models.
 */
public class WhisperModelRepository(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient(),
    private val baseUrlOverride: String? = null,
    private val checksumVerifier: (file: File, expectedSha256: String) -> Boolean = ::defaultVerifyChecksum,
) : ModelRepository {
    private val modelsDir = File(context.filesDir, "offlinecap/models").apply { mkdirs() }
    private val activeDownloads = mutableMapOf<WhisperModel, Flow<ModelState>>()
    private val downloadMutex = Mutex()

    override fun state(model: WhisperModel): Flow<ModelState> =
        flow {
            val file = getModelFile(model)
            if (file.exists() && checksumVerifier(file, model.sha256)) {
                emit(ModelState.Ready(ModelFile(file.absolutePath, file.length())))
            } else {
                emit(ModelState.Missing)
            }
        }.flowOn(Dispatchers.IO)

    override fun download(model: WhisperModel): Flow<ModelState> =
        flow {
            val sharedFlow =
                downloadMutex.withLock {
                    activeDownloads.getOrPut(model) {
                        createDownloadFlow(model)
                    }
                }
            emitAll(sharedFlow)
        }.flowOn(Dispatchers.IO)

    private fun createDownloadFlow(model: WhisperModel): Flow<ModelState> {
        val shared = MutableSharedFlow<ModelState>(replay = 1)

        // We actually want a cold flow that can be shared.
        // For simplicity in this implementation, we return a new flow that performs the download.
        // A better production version would use shareIn on a scope.
        return flow {
            try {
                val file = getModelFile(model)
                val tempFile = File(file.absolutePath + ".part")

                val url =
                    if (baseUrlOverride != null) {
                        "$baseUrlOverride/${model.modelName}.bin"
                    } else {
                        model.downloadUrl
                    }
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed to download model: ${response.code}")
                    }

                    val body = response.body ?: throw Exception("Empty response body")
                    val totalBytes = body.contentLength()

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalRead = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalRead += bytesRead
                                if (totalBytes > 0) {
                                    emit(ModelState.Downloading(totalRead.toFloat() / totalBytes))
                                }
                            }
                        }
                    }
                }

                if (checksumVerifier(tempFile, model.sha256)) {
                    tempFile.renameTo(file)
                    emit(ModelState.Ready(ModelFile(file.absolutePath, file.length())))
                } else {
                    tempFile.delete()
                    emit(ModelState.Failed(ModelError.ChecksumMismatch))
                }
            } catch (e: Exception) {
                emit(ModelState.Failed(ModelError.NetworkError(e)))
            } finally {
                downloadMutex.withLock {
                    activeDownloads.remove(model)
                }
            }
        }
    }

    override suspend fun delete(model: WhisperModel) {
        val file = getModelFile(model)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun getModelFile(model: WhisperModel): File = File(modelsDir, "${model.modelName}.bin")
}
