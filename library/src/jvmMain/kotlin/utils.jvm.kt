package io.github.kotlin.fibonacci

actual fun getCurrentTime(): Long {
    return System.currentTimeMillis()
}