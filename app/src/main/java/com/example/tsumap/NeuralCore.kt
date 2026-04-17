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
}

class DenseLayer(
    private val inputSize: Int,
    private val outputSize: Int,
    random: Random
) {
    private val weights = FloatArray(inputSize * outputSize)
    private val bias = FloatArray(outputSize)

    init {
        val limit = sqrt(6f / (inputSize + outputSize))
        for (i in weights.indices) {
            weights[i] = random.nextFloat() * 2f * limit - limit
        }
    }

    fun forward(input: Array<FloatArray>): Array<FloatArray> {
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

    fun getWeights(): FloatArray = weights.copyOf()
    fun getBias(): FloatArray = bias.copyOf()
    fun setWeights(w: FloatArray) {
        w.copyInto(weights)
    }

    fun setBias(b: FloatArray) {
        b.copyInto(bias)
    }
}

class ReLULayer {
    fun forward(input: Array<FloatArray>): Array<FloatArray> {
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

    fun predict(input: FloatArray): Int {
        val batch = arrayOf(input)
        val z1 = dense1.forward(batch)
        val a1 = relu.forward(z1)
        val logits = dense2.forward(a1)
        return MathOps.argMax(logits[0])
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