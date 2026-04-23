package com.danielebonaldo.dashboard.clockcompassdial

actual fun localTimezoneOffsetMs(): Long =
    java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
