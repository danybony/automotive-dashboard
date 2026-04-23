package com.danielebonaldo.dashboard.clockcompassdial

actual fun localTimezoneOffsetMs(): Long {
    val ms: Double = js("-new Date().getTimezoneOffset() * 60000")
    return ms.toLong()
}
