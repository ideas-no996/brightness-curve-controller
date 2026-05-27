package com.evan.brightnesscurve.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.evan.brightnesscurve.data.BrightnessPoint
import com.evan.brightnesscurve.data.BrightnessRevisionEntity

@Composable
internal fun EditorTab(
    state: MainUiState,
    padding: PaddingValues,
    onCopyPreset: (Long) -> Unit,
    onSavePreset: (Long, List<BrightnessPoint>, Float, Float, Float) -> Unit,
    onRestoreRevision: (Long) -> Unit
) {
    val preset = state.editorPreset

    if (preset == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("还没有可编辑的预设")
        }
        return
    }

    val draftPoints = remember(preset.id, preset.updatedAt) {
        mutableStateListOf(*preset.points.map { DraftPoint(it.lux.toString(), it.brightnessPercent) }.toTypedArray())
    }
    var smoothing by remember(preset.id, preset.updatedAt) { mutableFloatStateOf(preset.smoothingLevel) }
    var maxChange by remember(preset.id, preset.updatedAt) { mutableFloatStateOf(preset.maxChangePerUpdate) }
    var minDelta by remember(preset.id, preset.updatedAt) { mutableFloatStateOf(preset.minUpdateDelta) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(preset.name, style = MaterialTheme.typography.titleMedium)
                    if (preset.isBuiltIn) {
                        Text("内置预设不能直接修改。复制后会生成自定义版本。")
                        Button(onClick = { onCopyPreset(preset.id) }) { Text("复制为自定义预设") }
                    } else {
                        Text("修改控制点后保存，会自动生成一个可回滚版本。")
                    }
                }
            }
        }

        items(draftPoints.size, key = { index -> "$index-${draftPoints[index].luxText}" }) { index ->
            val point = draftPoints[index]
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = point.luxText,
                        onValueChange = { draftPoints[index] = point.copy(luxText = it) },
                        label = { Text("环境光 lux") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !preset.isBuiltIn
                    )
                    Text("亮度：${point.brightnessPercent.toInt()}%")
                    Slider(
                        value = point.brightnessPercent,
                        onValueChange = { draftPoints[index] = point.copy(brightnessPercent = it) },
                        valueRange = 1f..100f,
                        enabled = !preset.isBuiltIn
                    )
                    if (!preset.isBuiltIn && draftPoints.size > 3) {
                        TextButton(onClick = { draftPoints.removeAt(index) }) { Text("删除这个点") }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("变化策略", style = MaterialTheme.typography.titleMedium)
                    Text("平滑强度：${"%.2f".format(smoothing)}")
                    Slider(value = smoothing, onValueChange = { smoothing = it }, valueRange = 0.05f..0.95f, enabled = !preset.isBuiltIn)
                    Text("每次最多变化：${maxChange.toInt()}%")
                    Slider(value = maxChange, onValueChange = { maxChange = it }, valueRange = 1f..30f, enabled = !preset.isBuiltIn)
                    Text("最小更新差值：${minDelta.toInt()}%")
                    Slider(value = minDelta, onValueChange = { minDelta = it }, valueRange = 1f..20f, enabled = !preset.isBuiltIn)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    enabled = !preset.isBuiltIn,
                    onClick = {
                        val nextLux = draftPoints.lastOrNull()?.luxText?.toFloatOrNull()?.let { it * 2f } ?: 100f
                        draftPoints.add(DraftPoint(nextLux.toString(), 20f))
                    }
                ) {
                    Text("新增点")
                }
                Button(
                    enabled = !preset.isBuiltIn,
                    onClick = {
                        val points = draftPoints.mapNotNull {
                            val lux = it.luxText.toFloatOrNull()
                            if (lux == null) null else BrightnessPoint(lux, it.brightnessPercent)
                        }
                        onSavePreset(preset.id, points, smoothing, maxChange, minDelta)
                    }
                ) {
                    Text("保存曲线")
                }
            }
        }

        item {
            RevisionList(revisions = state.revisions, onRestoreRevision = onRestoreRevision)
        }
    }
}

@Composable
private fun RevisionList(
    revisions: List<BrightnessRevisionEntity>,
    onRestoreRevision: (Long) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("历史版本", style = MaterialTheme.typography.titleMedium)
            if (revisions.isEmpty()) {
                Text("保存或校准后会在这里出现可回滚版本。")
            } else {
                revisions.take(8).forEach { revision ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(formatTime(revision.createdAt), fontWeight = FontWeight.Bold)
                            Text(revision.note)
                        }
                        TextButton(onClick = { onRestoreRevision(revision.id) }) {
                            Text("恢复")
                        }
                    }
                }
            }
        }
    }
}

private data class DraftPoint(
    val luxText: String,
    val brightnessPercent: Float
)
