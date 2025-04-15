package io.github.kotlin.fibonacci

actual fun getCurrentTime(): Long {
    return System.currentTimeMillis()
}

actual fun Collector.getClassName(): String {
    return this.javaClass.simpleName
}