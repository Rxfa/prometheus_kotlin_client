package io.github.kotlin.fibonacci

public actual fun Collector.getClassName(): String {
    return this.javaClass.simpleName
}