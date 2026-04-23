package com.danielebonaldo.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.danielebonaldo.dashboard.clockcompassdial.DialMode
import com.danielebonaldo.dashboard.clockcompassdial.UiState


@Composable
fun ControlsColumn(
    uiState: UiState,
    onModeSelected: (DialMode) -> Unit,
    onStartStop: () -> Unit,
    onReset: () -> Unit
) {
    Column(Modifier.fillMaxHeight()) {
        ModeSelector(uiState, onModeSelected)

        AnimatedVisibility(
            visible = uiState.dialMode == DialMode.Stopwatch,
            enter = slideIn { IntOffset(-it.width, 0) },
            exit = slideOut { IntOffset(-it.width, 0) }) {
            Column {
                Spacer(Modifier.height(32.dp))
                Button(onClick = onStartStop) {
                    Text("Start/Stop")
                }
                Button(onClick = onReset) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
fun ControlsRow(
    uiState: UiState,
    onModeSelected: (DialMode) -> Unit,
    onStartStop: () -> Unit,
    onReset: () -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            ModeSelector(uiState, onModeSelected)
        }

        AnimatedVisibility(
            visible = uiState.dialMode == DialMode.Stopwatch,
            enter = slideIn { IntOffset(it.width, 0) },
            exit = slideOut { IntOffset(it.width, 0) },
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Spacer(Modifier.height(32.dp))
                Button(onClick = onStartStop) {
                    Text("Start/Stop")
                }
                Button(onClick = onReset) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(
    uiState: UiState,
    onModeSelected: (DialMode) -> Unit
) {
    DialMode.entries.forEach { mode ->
        Button(
            border = BorderStroke(
                if (uiState.dialMode == mode) 2.dp else 0.dp,
                if (uiState.dialMode == mode) Color.White else Color.Transparent
            ),
            onClick = { onModeSelected(mode) }
        ) {
            Text(mode.name)
        }
    }
}
