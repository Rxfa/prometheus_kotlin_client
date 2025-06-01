package io.github.kotlin.fibonacci

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


class Buffer {
    //TODO(nned  to )
    private val bufferActiveBit = 1L shl 63
    private var observationCount = atomic(0L)
    private var observationBuffer = DoubleArray(0)
    private var bufferPos = 0
    private var reset = false

    private val appendLock = Mutex()
    private val runLock = Mutex()
    //private val bufferCond =


    suspend fun append(value: Double): Boolean = withContext(Dispatchers.IO) {
       val count = observationCount.incrementAndGet()
        if ((count and bufferActiveBit) == 0L) {
            false // sign bit not set -> buffer not active.
        } else {
            doAppend(value)
            true
        }

    }

    private suspend fun doAppend(amount: Double) {
        appendLock.withLock {
            if (bufferPos >= observationBuffer.size) {
                observationBuffer = observationBuffer.copyOf(observationBuffer.size + 128)
            }
            observationBuffer[bufferPos] = amount
            bufferPos++
        }
    }

    fun reset() {
        reset = true
    }


    suspend fun <T> run(
        complete: (Long) -> Boolean,
        createResult: () -> T,
        observeFunction: (Double) -> Unit
    ): T = withContext(Dispatchers.Default) {
        val buffer: DoubleArray
        val bufferSize: Int
        val result: T

        runLock.withLock {
            val expectedCount = observationCount.getAndAdd(bufferActiveBit)

            while (!complete(expectedCount)) {
                kotlinx.coroutines.yield()
            }

            result = createResult()

            val expectedBufferSize: Int = if (reset) {
                val count = observationCount.getAndSet(0) and bufferActiveBit.inv()
                reset = false
                (count - expectedCount).toInt()
            } else {
                (observationCount.addAndGet(bufferActiveBit) - expectedCount).toInt()
            }

            appendLock.withLock {
                while (bufferPos < expectedBufferSize) {
                    kotlinx.coroutines.yield()
                }
            }

            buffer = observationBuffer
            bufferSize = bufferPos
            observationBuffer = DoubleArray(0)
            bufferPos = 0
        }

        for (i in 0 until bufferSize) {
            observeFunction(buffer[i])
        }

        return@withContext result
    }

}
