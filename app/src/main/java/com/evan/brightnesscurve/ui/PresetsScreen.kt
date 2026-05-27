package com.evan.brightnesscurve.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evan.brightnesscurve.data.BrightnessPreset

@Composable
internal fun PresetsTab(
    state: MainUiState,
    padding: PaddingValues,
    onActivatePreset: (Long) -> Unit,
    onCopyPreset: (Long) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onRenamePreset: (Long, String) -> Unit,
    onSelectEditorPreset: (Long) -> Unit
) {
    var renameTarget by remember { mutableStateOf<BrightnessPreset?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.presets, key = { it.id }) { preset ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(preset.name, style = MaterialTheme.typography.titleMedium)
                            Text(if (preset.isBuiltIn) "内置预设" else "自定义预设")
                        }
                        if (preset.isActive) {
                            FilterChip(selected = true, onClick = {}, label = { Text("启用中") })
                        }
                    }
                    Text("${preset.points.size} 个控制点 · 室内点约 ${preset.points.firstOrNull { it.lux >= 100f }?.brightnessPercent?.toInt() ?: "-"}%")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onSelectEditorPreset(preset.id) }) { Text("编辑") }
                        OutlinedButton(onClick = { onCopyPreset(preset.id) }) { Text("复制") }
                        if (!preset.isActive) {
                            Button(onClick = { onActivatePreset(preset.id) }) { Text("启用") }
                        }
                    }
                    if (!preset.isBuiltIn) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { renameTarget = preset }) { Text("重命名") }
                            TextButton(onClick = { onDeletePreset(preset.id) }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { preset ->
        RenameDialog(
            currentName = preset.name,
            onDismiss = { renameTarget = null },
            onSave = {
                onRenamePreset(preset.id, it)
                renameTarget = null
            }
        )
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名预设") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSave(name) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
