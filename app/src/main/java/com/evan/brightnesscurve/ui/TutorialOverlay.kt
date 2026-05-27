package com.evan.brightnesscurve.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun TutorialOverlay(
    onSkip: () -> Unit,
    onFinish: (Boolean) -> Unit
) {
    val steps = remember {
        listOf(
            TutorialStep(
                title = "跟着光线走",
                body = "环境变亮或变暗时，我会帮你调整屏幕亮度。"
            ),
            TutorialStep(
                title = "告诉我你的感觉",
                body = "觉得太暗、刚刚好或太亮，点一下就能校准。"
            ),
            TutorialStep(
                title = "慢慢变成你的曲线",
                body = "每次校准都会让亮度更贴近你的习惯。"
            ),
            TutorialStep(
                title = "随时交还给你",
                body = "你可以关掉自动控制，也能在设置里重看教程。"
            )
        )
    }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[stepIndex]
    val isLastStep = stepIndex == steps.lastIndex

    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(step.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(step.body)
                Text(
                    text = "第 ${stepIndex + 1} / ${steps.size} 步",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            if (isLastStep) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onFinish(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("以后每次打开都显示")
                    }
                    OutlinedButton(
                        onClick = { onFinish(false) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("以后不再自动显示")
                    }
                }
            } else {
                Button(onClick = { stepIndex += 1 }) {
                    Text("下一步")
                }
            }
        },
        dismissButton = {
            if (!isLastStep) {
                TextButton(onClick = onSkip) {
                    Text("跳过")
                }
            }
        }
    )
}

private data class TutorialStep(
    val title: String,
    val body: String
)
