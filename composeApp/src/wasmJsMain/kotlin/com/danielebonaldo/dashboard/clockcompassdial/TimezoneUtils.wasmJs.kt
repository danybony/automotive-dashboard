package com.danielebonaldo.dashboard.clockcompassdial

@JsFun("() => -new Date().getTimezoneOffset() * 60000.0")
private external fun jsTimezoneOffsetMs(): Double

actual fun localTimezoneOffsetMs(): Long = jsTimezoneOffsetMs().toLong()
