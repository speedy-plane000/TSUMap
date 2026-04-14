#!/usr/bin/env kotlin
/**
 * Standalone Kotlin script for training the digit classifier on a PC.
 *
 * Run from the project root directory:
 *   kotlin train_mnist.main.kts
 *
 * Or compile to a JAR and run:
 *   kotlinc -script train_mnist.main.kts
 */

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.math.sqrt
import kotlin.random.Random

// ===== Math helpers =====

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

    fun shuffleInPlace(indices: IntArray, random: Random) {
        for (i in indices.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = indices[i]
            indices[i] = indices[j]
            indices[j] = tmp
        }
    }
}

// ===== Layers =====

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
        for (k in weights.indices) weights[k] -= learningRate * gradW[k] * scale
        for (j in 0 until outputSize) bias[j] -= learningRate * gradB[j] * scale
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
    data class Result(val loss: Float, val gradLogits: Array<FloatArray>)

    fun forward(logits: Array<FloatArray>, labels: IntArray): Result {
        val batch = logits.size
        val classes = logits[0].size
        var loss = 0f
        val grad = Array(batch) { FloatArray(classes) }
        for (n in 0 until batch) {
            var maxLogit = logits[n][0]
            for (j in 1 until classes) if (logits[n][j] > maxLogit) maxLogit = logits[n][j]
            var sumExp = 0f
            for (j in 0 until classes) {
                val expv = kotlin.math.exp((logits[n][j] - maxLogit).toDouble()).toFloat()
                grad[n][j] = expv
                sumExp += expv
            }
            for (j in 0 until classes) grad[n][j] /= sumExp
            val y = labels[n]
            val p = grad[n][y].coerceAtLeast(1e-7f)
            loss -= kotlin.math.ln(p)
            grad[n][y] -= 1f
        }
        loss /= batch
        return Result(loss = loss, gradLogits = grad)
    }
}

// ===== Model =====

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

    fun predictBatch(inputs: Array<FloatArray>): IntArray {
        val z1 = dense1.forward(inputs)
        val a1 = relu.forward(z1)
        val logits = dense2.forward(a1)
        return IntArray(inputs.size) { i -> MathOps.argMax(logits[i]) }
    }

    fun saveToFile(file: java.io.File) {
        java.io.DataOutputStream(java.io.BufferedOutputStream(java.io.FileOutputStream(file))).use { out ->
            out.writeInt(1) // формат версии
            for (w in dense1.getWeights()) out.writeFloat(w)
            for (b in dense1.getBias())    out.writeFloat(b)
            for (w in dense2.getWeights()) out.writeFloat(w)
            for (b in dense2.getBias())    out.writeFloat(b)
        }
    }

    fun loadFromFile(file: java.io.File) {
        java.io.DataInputStream(java.io.BufferedInputStream(java.io.FileInputStream(file))).use { inp ->
            val version = inp.readInt()
            require(version == 1) { "Unknown model version: $version" }
            dense1.setWeights(FloatArray(dense1.getWeights().size) { inp.readFloat() })
            dense1.setBias(FloatArray(dense1.getBias().size) { inp.readFloat() })
            dense2.setWeights(FloatArray(dense2.getWeights().size) { inp.readFloat() })
            dense2.setBias(FloatArray(dense2.getBias().size) { inp.readFloat() })
        }
    }
}

// ===== MNIST loader (reads directly from the filesystem, no Android Context needed) =====

data class MnistDataset(val images: Array<FloatArray>, val labels: IntArray)

fun loadMnist(imagesFile: File, labelsFile: File, limit: Int? = null): MnistDataset {
    val img = DataInputStream(BufferedInputStream(FileInputStream(imagesFile)))
    val lbl = DataInputStream(BufferedInputStream(FileInputStream(labelsFile)))
    return img.use { images ->
        lbl.use { labels ->
            val imageMagic = images.readInt()
            require(imageMagic == 2051) { "Invalid image magic: $imageMagic (expected 2051)" }
            val numImages = images.readInt()
            val srcRows = images.readInt()
            val srcCols = images.readInt()

            val labelMagic = labels.readInt()
            require(labelMagic == 2049) { "Invalid label magic: $labelMagic (expected 2049)" }
            val numLabels = labels.readInt()

            require(numImages == numLabels) { "Images count ($numImages) != Labels count ($numLabels)" }
            require(srcRows == 28 && srcCols == 28) { "Expected 28x28, got ${srcRows}x${srcCols}" }

            val outputRows = 50
            val outputCols = 50
            val count = limit?.coerceIn(1, numImages) ?: numImages
            val imagesArr = Array(count) { FloatArray(outputRows * outputCols) }
            val labelsArr = IntArray(count)
            val srcImage = IntArray(srcRows * srcCols)

            for (i in 0 until count) {
                for (p in srcImage.indices) srcImage[p] = images.readUnsignedByte()
                labelsArr[i] = labels.readUnsignedByte()
                imagesArr[i] = resizeAndNormalize(srcImage, srcRows, srcCols, outputRows, outputCols)
            }

            MnistDataset(imagesArr, labelsArr)
        }
    }
}

fun resizeAndNormalize(
    src: IntArray, srcRows: Int, srcCols: Int, outRows: Int, outCols: Int
): FloatArray {
    val out = FloatArray(outRows * outCols)
    for (y in 0 until outRows) {
        val srcY = ((y.toFloat() / outRows) * srcRows).toInt().coerceIn(0, srcRows - 1)
        for (x in 0 until outCols) {
            val srcX = ((x.toFloat() / outCols) * srcCols).toInt().coerceIn(0, srcCols - 1)
            out[y * outCols + x] = src[srcY * srcCols + srcX] / 255f
        }
    }
    return out
}

fun evaluateAccuracy(model: DigitClassifier, images: Array<FloatArray>, labels: IntArray): Float {
    val preds = model.predictBatch(images)
    var correct = 0
    for (i in preds.indices) if (preds[i] == labels[i]) correct++
    return correct.toFloat() / preds.size
}

// ===== Entry point =====

val projectRoot = File(System.getProperty("user.dir") ?: ".")
val mnistDir = File(projectRoot, "app/src/main/assets/mnist")

println("MNIST directory: ${mnistDir.absolutePath}")
if (!mnistDir.exists()) {
    System.err.println("ERROR: MNIST directory not found.")
    System.err.println("Run this script from the project root (the folder that contains app/).")
    System.exit(1)
}

val trainImages = File(mnistDir, "train-images-idx3-ubyte")
val trainLabels = File(mnistDir, "train-labels-idx1-ubyte")
val testImages  = File(mnistDir, "t10k-images-idx3-ubyte")
val testLabels  = File(mnistDir, "t10k-labels-idx1-ubyte")

for (f in listOf(trainImages, trainLabels, testImages, testLabels)) {
    if (!f.exists()) {
        System.err.println("ERROR: File not found: ${f.absolutePath}")
        System.exit(1)
    }
}

println("Loading training data (10_000 samples)...")
val train = loadMnist(trainImages, trainLabels, limit = 10_000)

println("Loading test data (2_000 samples)...")
val test = loadMnist(testImages, testLabels, limit = 2_000)

val model = DigitClassifier(inputSize = 50 * 50, hiddenSize = 128, numClasses = 10, seed = 42)

val epochs      = 5
val batchSize   = 32
val learningRate = 0.01f
val rng = Random(42)
val n = train.images.size
val indices = IntArray(n) { it }

println("\nStarting training: $epochs epochs, batchSize=$batchSize, lr=$learningRate")
println("=".repeat(60))

for (epoch in 1..epochs) {
    MathOps.shuffleInPlace(indices, rng)
    var lossSum = 0f
    var steps = 0
    var start = 0
    while (start < n) {
        val end = minOf(start + batchSize, n)
        val bs = end - start
        val batchX = Array(bs) { i -> train.images[indices[start + i]] }
        val batchY = IntArray(bs) { i -> train.labels[indices[start + i]] }
        lossSum += model.trainOnBatch(batchX, batchY, learningRate)
        steps++
        start = end
    }

    val trainAcc = evaluateAccuracy(model, train.images, train.labels)
    val testAcc  = evaluateAccuracy(model, test.images,  test.labels)
    println(
        "Epoch %d/%d  loss=%.4f  train_acc=%.4f  test_acc=%.4f"
            .format(epoch, epochs, lossSum / steps, trainAcc, testAcc)
    )
}

println("=".repeat(60))
println("Training complete!")

val modelFile = File(projectRoot, "app/src/main/assets/digit_model.bin")
model.saveToFile(modelFile)
println("Model saved to: ${modelFile.absolutePath}")
