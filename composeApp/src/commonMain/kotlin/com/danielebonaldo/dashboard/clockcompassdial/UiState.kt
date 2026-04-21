package com.danielebonaldo.dashboard.clockcompassdial

sealed class UiState(val dialMode: DialMode) {
    data class Clock(val timeMillis: Long) : UiState(DialMode.Clock)

    sealed class Stopwatch : UiState(DialMode.Stopwatch) {
        data object Zero : Stopwatch()
        data class Running(val elapsedMillis: Long, val startMillis: Long) : Stopwatch()
        data class Paused(val elapsedMillis: Long) : Stopwatch()
    }

    data class Compass(val compassDegrees: Int) : UiState(DialMode.Compass)
}

enum class DialMode { Clock, Stopwatch, Compass }
