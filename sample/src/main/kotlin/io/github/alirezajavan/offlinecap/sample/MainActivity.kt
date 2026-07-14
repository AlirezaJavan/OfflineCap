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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.SubtitleCue
import io.github.alirezajavan.offlinecap.core.model.SubtitleFormat
import io.github.alirezajavan.offlinecap.core.model.WhisperModel
import io.github.alirezajavan.offlinecap.core.model.WordTiming

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

private enum class SampleTab(
    val label: String,
) {
    CAPTION("Caption"),
    MODULES("Modules"),
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SampleApp() {
    var selectedTab by remember { mutableStateOf(SampleTab.CAPTION) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == SampleTab.CAPTION,
                    onClick = { selectedTab = SampleTab.CAPTION },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    label = { Text("Caption") },
                )
                NavigationBarItem(
                    selected = selectedTab == SampleTab.MODULES,
                    onClick = { selectedTab = SampleTab.MODULES },
                    icon = { Icon(Icons.Filled.Build, contentDescription = null) },
                    label = { Text("Modules") },
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            SampleTab.CAPTION -> CaptionScreen(modifier = Modifier.padding(padding))
            SampleTab.MODULES -> ModulesScreen(modifier = Modifier.padding(padding))
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun CaptionScreen(
    modifier: Modifier = Modifier,
    viewModel: CaptionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val pickVideoLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri: Uri? ->
            uri?.let { viewModel.startCaptioning(it.toString()) }
        }

    val saveSubtitleLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.CreateDocument(
                    when (uiState.selectedFormat) {
                        SubtitleFormat.SRT -> "application/x-subrip"
                        SubtitleFormat.WEBVTT -> "text/vtt"
                        SubtitleFormat.JSON -> "application/json"
                    },
                ),
        ) { uri: Uri? ->
            uri?.let { viewModel.saveSubtitle(it) }
        }

    LazyColumn(
        modifier =
            modifier
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
                onToggleWordTimestamps = viewModel::setWordTimestampsEnabled,
                onDownload = viewModel::downloadModel,
            )
        }
        item {
            TranslationCard(
                targetLanguage = uiState.targetLanguage,
                enabled = !uiState.isProcessing,
                onSelectTargetLanguage = viewModel::setTargetLanguage,
            )
        }
        item {
            ActionsCard(
                uiState = uiState,
                onSelectFormat = viewModel::setSelectedFormat,
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
            item { TranscriptCard(title = "Transcript", cues = uiState.cues) }
        }
        if (uiState.translatedCues.isNotEmpty()) {
            item { TranscriptCard(title = "Translated", cues = uiState.translatedCues) }
        }
        uiState.subtitleContent?.let { content ->
            item {
                SubtitleCard(
                    subtitleContent = content,
                    language = uiState.targetLanguage,
                    format = uiState.selectedFormat,
                    onExport = {
                        val extension = uiState.selectedFormat.name.lowercase()
                        saveSubtitleLauncher.launch("captions.$extension")
                    },
                )
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

private data class TargetLanguageOption(
    val label: String,
    val language: LanguageTag?,
)

private val TARGET_LANGUAGE_OPTIONS =
    listOf(
        TargetLanguageOption("None", null),
        TargetLanguageOption("Persian", LanguageTag("fa")),
        TargetLanguageOption("Spanish", LanguageTag("es")),
        TargetLanguageOption("French", LanguageTag("fr")),
        TargetLanguageOption("German", LanguageTag("de")),
        TargetLanguageOption("Japanese", LanguageTag("ja")),
    )

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranslationCard(
    targetLanguage: LanguageTag?,
    enabled: Boolean,
    onSelectTargetLanguage: (LanguageTag?) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Translation (ML Kit)", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Optional: translate the transcript on-device after transcription.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TARGET_LANGUAGE_OPTIONS.forEach { option ->
                    FilterChip(
                        selected = targetLanguage == option.language,
                        onClick = { onSelectTargetLanguage(option.language) },
                        enabled = enabled,
                        label = { Text(option.label) },
                    )
                }
            }
        }
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
    onToggleWordTimestamps: (Boolean) -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Word-level timestamps", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Renders per-word chips in the transcript below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.wordTimestampsEnabled,
                    onCheckedChange = onToggleWordTimestamps,
                    enabled = !uiState.isProcessing,
                )
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionsCard(
    uiState: UiState,
    onSelectFormat: (SubtitleFormat) -> Unit,
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

            Text(text = "Output Format", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SubtitleFormat.entries.forEach { format ->
                    FilterChip(
                        selected = uiState.selectedFormat == format,
                        onClick = { onSelectFormat(format) },
                        enabled = !uiState.isProcessing,
                        label = { Text(format.name) },
                    )
                }
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
private fun TranscriptCard(
    title: String,
    cues: List<SubtitleCue>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "$title (${cues.size} segments)", style = MaterialTheme.typography.titleMedium)
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
                        if (cue.words.isNotEmpty()) {
                            WordChips(cue.words)
                        } else {
                            Text(text = cue.text.trim(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
private fun WordChips(words: List<WordTiming>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        words.forEach { word ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = word.text.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun SubtitleCard(
    subtitleContent: String,
    language: LanguageTag?,
    format: SubtitleFormat,
    onExport: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = if (language != null) "Subtitles ($format · $language)" else "Subtitles ($format)"
            Text(text = title, style = MaterialTheme.typography.titleMedium)
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
            FilledTonalButton(onClick = onExport) { Text("Export ${format.name}") }
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
