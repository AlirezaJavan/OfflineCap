package io.github.alirezajavan.offlinecap.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import io.github.alirezajavan.offlinecap.core.engine.AudioDecoder
import io.github.alirezajavan.offlinecap.core.model.PcmChunk
import io.github.alirezajavan.offlinecap.core.model.PcmSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "OfflineCap.Audio"

/**
 * Android-specific [AudioDecoder] using MediaExtractor and MediaCodec.
 */
public class MediaCodecAudioDecoder(
    private val context: Context,
) : AudioDecoder {
    override fun decode(
        videoUri: String,
        spec: PcmSpec,
    ): Flow<PcmChunk> =
        flow {
            val extractor = MediaExtractor()
            try {
                val uri = Uri.parse(videoUri)
                if (uri.scheme == "content") {
                    extractor.setDataSource(context, uri, null)
                } else {
                    extractor.setDataSource(videoUri)
                }
                val trackIndex = selectAudioTrack(extractor)
                if (trackIndex < 0) {
                    throw IllegalStateException("No audio track found in $videoUri")
                }
                extractor.selectTrack(trackIndex)

                val format = extractor.getTrackFormat(trackIndex)
                val durationMs =
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        format.getLong(MediaFormat.KEY_DURATION) / 1000
                    } else {
                        0L
                    }
                val codecName =
                    MediaCodecList(MediaCodecList.ALL_CODECS)
                        .findDecoderForFormat(format)
                val codec = MediaCodec.createByCodecName(codecName)

                codec.configure(format, null, null, 0)
                codec.start()

                Log.d(TAG, "decode start uri=$videoUri durationMs=$durationMs")
                val startTime = System.currentTimeMillis()

                val rawPcmFlow = decodeLoop(extractor, codec, spec, durationMs)
                PcmWindowing.window(rawPcmFlow).collect { emit(it) }

                codec.stop()
                codec.release()
                Log.d(TAG, "decode end elapsedMs=${System.currentTimeMillis() - startTime}")
            } finally {
                extractor.release()
            }
        }.flowOn(Dispatchers.IO)

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return i
        }
        return -1
    }

    private fun decodeLoop(
        extractor: MediaExtractor,
        codec: MediaCodec,
        spec: PcmSpec,
        durationMs: Long,
    ): Flow<PcmChunk> =
        flow {
            val bufferInfo = MediaCodec.BufferInfo()
            var isExtractorDone = false
            var isCodecDone = false

            // Resampler and Mixer will be initialized once we know source format
            var resampler: PcmResampler? = null
            var mixer: MonoMixer? = null

            while (!isCodecDone) {
                if (!isExtractorDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isExtractorDone = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputIndex >= 0) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isCodecDone = true
                    }

                    val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                    val pcmData = processOutputBuffer(outputBuffer, bufferInfo, codec.outputFormat)

                    // Lazy init processing components
                    if (resampler == null) {
                        val sourceRate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        val channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        resampler = PcmResampler(sourceRate, spec.sampleRate)
                        mixer = MonoMixer(channels)
                    }

                    val monoData = mixer!!.mix(pcmData)
                    val resampledData = resampler!!.resample(monoData)

                    if (resampledData.isNotEmpty()) {
                        emit(PcmChunk(resampledData, bufferInfo.presentationTimeUs / 1000, durationMs))
                    }

                    codec.releaseOutputBuffer(outputIndex, false)
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Format changed, we handle this lazily in next output buffer
                }
            }
        }

    private fun processOutputBuffer(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        format: MediaFormat,
    ): FloatArray {
        // Assume PCM_16BIT for now, which is common
        buffer.position(info.offset)
        buffer.limit(info.offset + info.size)

        val shortBuffer = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val floatArray = FloatArray(shortBuffer.remaining())
        for (i in floatArray.indices) {
            floatArray[i] = shortBuffer.get() / 32768.0f
        }
        return floatArray
    }
}
