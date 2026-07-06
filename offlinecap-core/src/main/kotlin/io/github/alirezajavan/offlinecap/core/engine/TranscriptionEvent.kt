package io.github.alirezajavan.offlinecap.core.engine

import io.github.alirezajavan.offlinecap.core.model.SubtitleCue

/**
 * Events emitted by the transcription engine.
 */
public sealed interface TranscriptionEvent {
    public data class Progress(
        val progress: Float,
    ) : TranscriptionEvent

    public data class Segment(
        val cue: SubtitleCue,
    ) : TranscriptionEvent
}
