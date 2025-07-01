package io.github.rxfa.prometheus.core

public class Exemplar {
    private val labels: List<String>
    private val value: Double
    private val timestamp: Long?

    public constructor(labels: List<String>, value: Double, timestamp: Long) {
        this.labels = labels
        this.value = value
        this.timestamp = timestamp
    }

    public constructor(labels: List<String>, value: Double) {
        this.labels = labels
        this.value = value
        this.timestamp = null
    }

    public fun getLabels(): List<String> {
        return labels
    }

    public fun getLabel(name: String): String? {
        return labels.find { it == name }
    }

    public fun getLabel(index: Int): String? {
        if (index < 0 || index >= labels.size) {
            return null
        }
        return labels[index]
    }

    public fun getValue(): Double {
        return value
    }

    public fun getTimestamp(): Long? {
        return timestamp
    }
}
