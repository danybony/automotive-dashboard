package com.danielebonaldo.dashboard.clockcompassdial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class MultiDialViewModel : ViewModel() {

    private val compassFlow = flow {
        var currentDegrees = 0
        while (true) {
            currentDegrees += Random.nextInt(30) - 20
            emit(currentDegrees)
            delay(1.seconds)
        }
    }
    val compassUiState = compassFlow.map {
        UiState.Compass(it)
    }.stateIn(
        viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UiState.Compass(0)
    )

    private val _stopwatchState = MutableStateFlow<UiState.Stopwatch>(UiState.Stopwatch.Zero)
    val stopwatchUiState: StateFlow<UiState> = _stopwatchState.asStateFlow()

    private val _clockState = MutableStateFlow(UiState.Clock(nowMillis()))
    val clockUiState: StateFlow<UiState> = _clockState.asStateFlow()


    private fun nowMillis(): Long {
        val now = Clock.System.now()
//        val timeZone = TimeZone.currentSystemDefault()
//        val localTime = now.toLocalDateTime(timeZone)
        return now.toEpochMilliseconds()
    }

    fun startStopWatch() {
        viewModelScope.launch {
            _stopwatchState.emit(
                when (val current = _stopwatchState.value) {
                    UiState.Stopwatch.Zero -> UiState.Stopwatch.Running(
                        elapsedMillis = 0L,
                        startMillis = nowMillis()
                    )

                   is UiState.Stopwatch.Running -> UiState.Stopwatch.Paused(
                       elapsedMillis = current.elapsedMillis + (nowMillis() - current.startMillis),
                   )

                   is UiState.Stopwatch.Paused -> UiState.Stopwatch.Running(
                       elapsedMillis = current.elapsedMillis,
                       startMillis = nowMillis()
                   )
                }
            )
        }
    }

    fun resetStopWatch() {
        viewModelScope.launch {
            _stopwatchState.emit(UiState.Stopwatch.Zero)
        }
    }
}
