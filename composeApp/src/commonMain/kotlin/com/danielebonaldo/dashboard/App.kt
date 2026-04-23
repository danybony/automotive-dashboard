package com.danielebonaldo.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danielebonaldo.dashboard.clockcompassdial.DialMode
import com.danielebonaldo.dashboard.clockcompassdial.MorphingDial
import com.danielebonaldo.dashboard.clockcompassdial.MultiDialViewModel

@Composable
@Preview
fun App() {
    AutomotiveDashboardTheme {
        val viewModel = viewModel { MultiDialViewModel() }
        var selectedMode by remember { mutableStateOf(DialMode.Clock) }
        val uiState by when (selectedMode) {
            DialMode.Clock -> viewModel.clockUiState.collectAsState()
            DialMode.Stopwatch -> viewModel.stopwatchUiState.collectAsState()
            DialMode.Compass -> viewModel.compassUiState.collectAsState()
        }
        var landscape by remember { mutableStateOf(false) }

        Box(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
                .onGloballyPositioned {
                    landscape = it.size.width > it.size.height
                }) {
        }

        if (landscape) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlsColumn(
                    uiState = uiState,
                    onModeSelected = {
                        viewModel.updateClock()
                        selectedMode = it
                    },
                    onStartStop = { viewModel.startStopWatch() },
                    onReset = { viewModel.resetStopWatch() }
                )
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MorphingDial(uiState)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                ControlsRow(
                    uiState = uiState,
                    onModeSelected = {
                        viewModel.updateClock()
                        selectedMode = it
                    },
                    onStartStop = { viewModel.startStopWatch() },
                    onReset = { viewModel.resetStopWatch() }
                )
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MorphingDial(uiState)
                }
            }
        }
    }
}
