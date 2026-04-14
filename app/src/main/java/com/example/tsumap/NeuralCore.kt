package com.example.tsumap

import kotlin.math.sqrt
import kotlin.random.Random

object MathOps {

    fun argMax(values: FloatArray): Int {
        var bestIdx = 0
        var bestVal = values[0]
        for (i in 1 until values.size) {
            if (values[i] > bestVal) {
                bestVal = values[i]
                bestIdx = i
            }
        }
        return bestIdx
    }

    fun shuffleInPlace(indices: IntArray, random: kotlin.random.Random) {
        for (i in indices.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = indices[i]
            indices[i] = indices[j]
            indices[j] = tmp
        }
    }
}

class DenseLayer(
    private val inputSize: Int,
    private val outputSize: Int,
    random: Random
) {
    private val weights = FloatArray(inputSize * outputSize)
    private val bias = FloatArray(outputSize)

    private lateinit var lastInput: Array<FloatArray>

    init {
        val limit = sqrt(6f / (inputSize + outputSize))
        for (i in weights.indices) {
            weights[i] = random.nextFloat() * 2f * limit - limit
        }
    }

    fun forward(input: Array<FloatArray>): Array<FloatArray> {
        lastInput = input
        val batch = input.size
        val out = Array(batch) { FloatArray(outputSize) }

        for (n in 0 until batch) {
            for (j in 0 until outputSize) {
                var sum = bias[j]
                var wIdx = j
                for (i in 0 until inputSize) {
                    sum += input[n][i] * weights[wIdx]
                    wIdx += outputSize
                }
                out[n][j] = sum
            }
        }

        return out
    }

    fun backward(gradOutput: Array<FloatArray>, learningRate: Float): Array<FloatArray> {
        val batch = gradOutput.size
        val gradInput = Array(batch) { FloatArray(inputSize) }
        val gradW = FloatArray(weights.size)
        val gradB = FloatArray(outputSize)

        for (n in 0 until batch) {
            for (j in 0 until outputSize) {
                val go = gradOutput[n][j]
                gradB[j] += go

                var wIdx = j
                for (i in 0 until inputSize) {
                    gradW[wIdx] += lastInput[n][i] * go
                    gradInput[n][i] += weights[wIdx] * go
                    wIdx += outputSize
                }
            }
        }

        val scale = 1f / batch
        for (k in weights.indices) {
            weights[k] -= learningRate * gradW[k] * scale
        }
        for (j in 0 until outputSize) {
            bias[j] -= learningRate * gradB[j] * scale
        }

        return gradInput
    }

    fun getWeights(): FloatArray = weights.copyOf()
    fun getBias(): FloatArray = bias.copyOf()
    fun setWeights(w: FloatArray) { w.copyInto(weights) }
    fun setBias(b: FloatArray) { b.copyInto(bias) }
}

class ReLULayer {
    private lateinit var lastInput: Array<FloatArray>

    fun forward(input: Array<FloatArray>): Array<FloatArray> {
        lastInput = input
        val batch = input.size
        val dim = input[0].size
        val out = Array(batch) { FloatArray(dim) }

        for (n in 0 until batch) {
            for (d in 0 until dim) {
                val v = input[n][d]
                out[n][d] = if (v > 0f) v else 0f
            }
        }

        return out
    }

    fun backward(gradOutput: Array<FloatArray>): Array<FloatArray> {
        val batch = gradOutput.size
        val dim = gradOutput[0].size
        val gradInput = Array(batch) { FloatArray(dim) }

        for (n in 0 until batch) {
            for (d in 0 until dim) {
                gradInput[n][d] = if (lastInput[n][d] > 0f) gradOutput[n][d] else 0f
            }
        }

        return gradInput
    }
}

object SoftmaxCrossEntropy {

    data class Result(
        val loss: Float,
        val gradLogits: Array<FloatArray>
    )

    fun forward(logits: Array<FloatArray>, labels: IntArray): Result {
        val batch = logits.size
        val classes = logits[0].size

        var loss = 0f
        val grad = Array(batch) { FloatArray(classes) }

        for (n in 0 until batch) {
            var maxLogit = logits[n][0]
            for (j in 1 until classes) {
                if (logits[n][j] > maxLogit) maxLogit = logits[n][j]
            }

            var sumExp = 0f
            for (j in 0 until classes) {
                val expv = kotlin.math.exp((logits[n][j] - maxLogit).toDouble()).toFloat()
                grad[n][j] = expv
                sumExp += expv
            }

            for (j in 0 until classes) {
                grad[n][j] /= sumExp
            }

            val y = labels[n]
            val p = grad[n][y].coerceAtLeast(1e-7f)
            loss -= kotlin.math.ln(p)
            grad[n][y] -= 1f
        }

        loss /= batch
        return Result(loss = loss, gradLogits = grad)
    }
}

class DigitClassifier(
    inputSize: Int = 50 * 50,
    hiddenSize: Int = 128,
    numClasses: Int = 10,
    seed: Int = 42
) {
    private val random = Random(seed)
    private val dense1 = DenseLayer(inputSize = inputSize, outputSize = hiddenSize, random = random)
    private val relu = ReLULayer()
    private val dense2 = DenseLayer(inputSize = hiddenSize, outputSize = numClasses, random = random)

    fun trainOnBatch(batchX: Array<FloatArray>, batchY: IntArray, learningRate: Float): Float {
        val z1 = dense1.forward(batchX)
        val a1 = relu.forward(z1)
        val logits = dense2.forward(a1)

        val (loss, gradLogits) = SoftmaxCrossEntropy.forward(logits, batchY)

        val gradA1 = dense2.backward(gradLogits, learningRate)
        val gradZ1 = relu.backward(gradA1)
        dense1.backward(gradZ1, learningRate)

        return loss
    }

    fun predict(input: FloatArray): Int {
        val batch = arrayOf(input)
        val z1 = dense1.forward(batch)
        val a1 = relu.forward(z1)
        val logits = dense2.forward(a1)
        return MathOps.argMax(logits[0])
    }

    fun predictBatch(inputs: Array<FloatArray>): IntArray {
        val z1 = dense1.forward(inputs)
        val a1 = relu.forward(z1)
        val logits = dense2.forward(a1)
        return IntArray(inputs.size) { i -> MathOps.argMax(logits[i]) }
    }

    fun loadFromAssets(context: android.content.Context, fileName: String = "digit_model.bin") {
        java.io.DataInputStream(
            java.io.BufferedInputStream(context.assets.open(fileName))
        ).use { inp ->
            val version = inp.readInt()
            require(version == 1) { "Unknown model version: $version" }
            dense1.setWeights(FloatArray(dense1.getWeights().size) { inp.readFloat() })
            dense1.setBias(FloatArray(dense1.getBias().size) { inp.readFloat() })
            dense2.setWeights(FloatArray(dense2.getWeights().size) { inp.readFloat() })
            dense2.setBias(FloatArray(dense2.getBias().size) { inp.readFloat() })
        }
    }
}