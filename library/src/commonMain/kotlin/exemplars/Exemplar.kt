package io.github.kotlin.fibonacci.exemplars

public class Exemplar {
    private final val labels:List<String>
    private final val value: Double
    private final val timestamp: Long?


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