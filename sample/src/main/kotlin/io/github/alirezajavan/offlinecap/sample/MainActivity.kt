package io.github.alirezajavan.offlinecap.sample

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import io.github.alirezajavan.offlinecap.core.model.WhisperModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SampleApp()
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SampleApp(viewModel: CaptionViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pickVideoLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri: Uri? ->
            uri?.let { viewModel.startCaptioning(it.toString()) }
        }
    val saveSrtLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/x-subrip"),
        ) { uri: Uri? ->
            uri?.let { viewModel.saveSubtitle(it) }
        }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header() }
        item {
            ModelCard(
                uiState = uiState,
                onSelectModel = viewModel::setModel,
                onDownload = viewModel::downloadModel,
            )
        }
        item {
            ActionsCard(
                uiState = uiState,
                onPickVideo = {
                    pickVideoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                    )
                },
                onStop = viewModel::stopCaptioning,
            )
        }
        if (uiState.stage != CaptionStage.IDLE) {
            item { ProgressCard(uiState) }
        }
        if (uiState.cues.isNotEmpty()) {
            item { TranscriptCard(uiState.cues) }
        }
        uiState.subtitleContent?.let { content ->
            item {
                SubtitleCard(
                    subtitleContent = content,
                    onExport = { saveSrtLauncher.launch("captions.srt") },
                )
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun Header() {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = "OfflineCap",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "On-device video captioning — fully offline",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class ModelGroup(
    val label: String,
    val variants: List<WhisperModel>,
)

private val MODEL_GROUPS =
    listOf(
        ModelGroup("Tiny", listOf(WhisperModel.TINY_Q5_1, WhisperModel.TINY_Q8_0, WhisperModel.TINY)),
        ModelGroup("Base", listOf(WhisperModel.BASE_Q5_1, WhisperModel.BASE_Q8_0, WhisperModel.BASE)),
        ModelGroup("Small", listOf(WhisperModel.SMALL_Q5_1, WhisperModel.SMALL_Q8_0, WhisperModel.SMALL)),
        ModelGroup("Medium", listOf(WhisperModel.MEDIUM_Q5_0, WhisperModel.MEDIUM_Q8_0, WhisperModel.MEDIUM)),
        ModelGroup(
            "Large v3 Turbo",
            listOf(WhisperModel.LARGE_V3_TURBO_Q5_0, WhisperModel.LARGE_V3_TURBO_Q8_0, WhisperModel.LARGE_V3_TURBO),
        ),
    )

private fun WhisperModel.variantLabel(): String =
    when {
        modelName.endsWith("q5_0") -> "Q5_0"
        modelName.endsWith("q5_1") -> "Q5_1"
        modelName.endsWith("q8_0") -> "Q8_0"
        else -> "F16"
    }

private fun WhisperModel.sizeLabel(): String = "${sizeBytes / 1_000_000} MB"

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelCard(
    uiState: UiState,
    onSelectModel: (WhisperModel) -> Unit,
    onDownload: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Whisper Model", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Quantized (Q5/Q8) variants are 2-3x faster than F16 at a small accuracy cost.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            MODEL_GROUPS.forEach { group ->
                Text(text = group.label, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.variants.forEach { model ->
                        FilterChip(
                            selected = uiState.whisperModel == model,
                            onClick = { onSelectModel(model) },
                            enabled = !uiState.isProcessing,
                            label = { Text("${model.variantLabel()} · ${model.sizeLabel()}") },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            ModelStateRow(uiState = uiState, onDownload = onDownload)
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ModelStateRow(
    uiState: UiState,
    onDownload: () -> Unit,
) {
    when (val state = uiState.modelState) {
        is ModelState.Missing -> {
            Text(
                text = "${uiState.whisperModel.modelName} is not downloaded yet.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onDownload) { Text("Download Model") }
        }
        is ModelState.Downloading -> {
            LabelledProgress(label = "Downloading ${uiState.whisperModel.modelName}", progress = state.progress)
        }
        is ModelState.Ready -> {
            Text(
                text = "${uiState.whisperModel.modelName} ready ✓",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        is ModelState.Failed -> {
            Text(
                text = "Download failed: ${state.error}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onDownload) { Text("Retry Download") }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ActionsCard(
    uiState: UiState,
    onPickVideo: () -> Unit,
    onStop: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Captioning", style = MaterialTheme.typography.titleMedium)
            uiState.activeModel?.let {
                Text(
                    text = "Using model: ${it.modelName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPickVideo,
                    enabled = uiState.modelState is ModelState.Ready && !uiState.isProcessing,
                ) {
                    Text("Pick Video & Caption")
                }
                if (uiState.isProcessing) {
                    OutlinedButton(onClick = onStop) { Text("Stop") }
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ProgressCard(uiState: UiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Progress", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = uiState.stage.label(),
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        when (uiState.stage) {
                            CaptionStage.FAILED -> MaterialTheme.colorScheme.error
                            CaptionStage.COMPLETED -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.tertiary
                        },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            LabelledProgress(label = "Audio extraction", progress = uiState.extractionProgress)
            Spacer(modifier = Modifier.height(8.dp))
            LabelledProgress(label = "Transcription", progress = uiState.transcriptionProgress)

            uiState.remainingTimeMs?.let { remainingMs ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Estimated time remaining: ${formatDuration(remainingMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            uiState.elapsedMs?.let { elapsedMs ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Finished in ${formatDuration(elapsedMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun LabelledProgress(
    label: String,
    progress: Float,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { progress },
        modifier =
            Modifier
                .fillMaxWidth()
                .height(8.dp),
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun TranscriptCard(cues: List<SubtitleCue>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Transcript (${cues.size} segments)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.height(260.dp)) {
                items(cues) { cue ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "${formatTimestamp(cue.startMs)} → ${formatTimestamp(cue.endMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(text = cue.text.trim(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun SubtitleCard(
    subtitleContent: String,
    onExport: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Subtitles (SRT)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                        ).padding(8.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = subtitleContent,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(onClick = onExport) { Text("Export SRT") }
        }
    }
}

private fun CaptionStage.label(): String =
    when (this) {
        CaptionStage.IDLE -> "Idle"
        CaptionStage.EXTRACTING -> "Extracting audio…"
        CaptionStage.TRANSCRIBING -> "Transcribing…"
        CaptionStage.TRANSLATING -> "Translating…"
        CaptionStage.COMPLETED -> "Completed ✓"
        CaptionStage.FAILED -> "Failed"
    }

private fun formatTimestamp(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%02d:%02d.%03d".format(totalSeconds / 60, totalSeconds % 60, ms % 1000)
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
