package io.github.alirezajavan.offlinecap.sample

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alirezajavan.offlinecap.core.lang.LanguageTag
import io.github.alirezajavan.offlinecap.core.model.ModelState
import io.github.alirezajavan.offlinecap.core.model.SubtitleFormat
import io.github.alirezajavan.offlinecap.subtitle.CueMergeOptions

private val MERGE_OPTION_PRESETS =
    listOf(
        "Tight (32/1)" to CueMergeOptions(maxCharsPerLine = 32, maxLines = 1),
        "Default (42/2)" to CueMergeOptions(),
        "Loose (60/2)" to CueMergeOptions(maxCharsPerLine = 60, maxLines = 2),
    )

private val TRANSLATE_TARGETS =
    listOf("Persian" to LanguageTag("fa"), "Spanish" to LanguageTag("es"), "French" to LanguageTag("fr"))

/**
 * Demonstrates using `:offlinecap-transcribe`, `:offlinecap-subtitle`, and `:offlinecap-lingua`
 * standalone, without the `:offlinecap` facade — see [ModulesViewModel].
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun ModulesScreen(
    modifier: Modifier = Modifier,
    viewModel: ModulesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val pickMediaLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri: Uri? ->
            uri?.let { viewModel.transcribe(it.toString()) }
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Standalone modules",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "offlinecap-transcribe + offlinecap-subtitle + offlinecap-lingua, used directly.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TranscribeCard(
            uiState = uiState,
            onDownloadModel = viewModel::downloadModel,
            onPickMedia = {
                pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
            onSelectMergeOptions = viewModel::setMergeOptions,
            onSelectFormat = viewModel::setSubtitleFormat,
        )

        TranslateCard(
            uiState = uiState,
            onInputChange = viewModel::setTranslationInput,
            onSelectTarget = viewModel::setTargetLanguage,
            onTranslate = viewModel::translate,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranscribeCard(
    uiState: ModulesUiState,
    onDownloadModel: () -> Unit,
    onPickMedia: () -> Unit,
    onSelectMergeOptions: (CueMergeOptions) -> Unit,
    onSelectFormat: (SubtitleFormat) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Audio → Transcript → Subtitle", style = MaterialTheme.typography.titleMedium)
            Spacer4dp()

            when (val state = uiState.modelState) {
                is ModelState.Missing -> Button(onClick = onDownloadModel) { Text("Download Tiny model") }
                is ModelState.Downloading ->
                    Text("Downloading… ${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                is ModelState.Ready -> Text("Model ready ✓", style = MaterialTheme.typography.bodyMedium)
                is ModelState.Failed -> Button(onClick = onDownloadModel) { Text("Retry download") }
            }
            Spacer4dp()

            Button(
                onClick = onPickMedia,
                enabled = uiState.modelState is ModelState.Ready && !uiState.isTranscribing,
            ) {
                Text(if (uiState.isTranscribing) "Transcribing…" else "Pick video & transcribe")
            }

            uiState.transcribeError?.let {
                Spacer4dp()
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (uiState.rawCues.isNotEmpty()) {
                Spacer4dp()
                Text("Cue merge options", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MERGE_OPTION_PRESETS.forEach { (label, options) ->
                        FilterChip(
                            selected = uiState.mergeOptions == options,
                            onClick = { onSelectMergeOptions(options) },
                            label = { Text(label) },
                        )
                    }
                }
                Spacer4dp()
                Text("Format", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubtitleFormat.entries.forEach { format ->
                        FilterChip(
                            selected = uiState.subtitleFormat == format,
                            onClick = { onSelectFormat(format) },
                            label = { Text(format.name) },
                        )
                    }
                }
            }

            uiState.subtitleContent?.let { content ->
                Spacer4dp()
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    Text(text = content, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranslateCard(
    uiState: ModulesUiState,
    onInputChange: (String) -> Unit,
    onSelectTarget: (LanguageTag) -> Unit,
    onTranslate: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Translate", style = MaterialTheme.typography.titleMedium)
            Spacer4dp()
            OutlinedTextField(
                value = uiState.translationInput,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Source text (English)") },
            )
            Spacer4dp()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TRANSLATE_TARGETS.forEach { (label, language) ->
                    FilterChip(
                        selected = uiState.targetLanguage == language,
                        onClick = { onSelectTarget(language) },
                        label = { Text(label) },
                    )
                }
            }
            Spacer4dp()
            Button(onClick = onTranslate, enabled = !uiState.isTranslating) {
                Text(if (uiState.isTranslating) "Translating…" else "Translate")
            }
            uiState.translationOutput?.let {
                Spacer4dp()
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            uiState.translationError?.let {
                Spacer4dp()
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun Spacer4dp() {
    androidx.compose.foundation.layout
        .Spacer(modifier = Modifier.height(8.dp))
}
