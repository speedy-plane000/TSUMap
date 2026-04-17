#!/usr/bin/env kotlin

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
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

    fun shuffleInPlace(indices: IntArray, random: Random) {
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

    private val velW = FloatArray(inputSize * outputSize)
    private val velB = FloatArray(outputSize)

    companion object {
        private const val MOMENTUM = 0.9f
        private const val WEIGHT_DECAY = 1e-4f
    }

    init {
        val std = sqrt(2f / inputSize)
        for (i in weights.indices) {
            val u1 = random.nextFloat().coerceAtLeast(1e-7f)
            val u2 = random.nextFloat()
            val z = sqrt(-2f * kotlin.math.ln(u1)) * kotlin.math.cos(2f * Math.PI.toFloat() * u2)
            weights[i] = z * std
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
            val g = gradW[k] * scale + WEIGHT_DECAY * weights[k]
            velW[k] = MOMENTUM * velW[k] - learningRate * g
            weights[k] += velW[k]
        }
        for (j in 0 until outputSize) {
            val g = gradB[j] * scale
            velB[j] = MOMENTUM * velB[j] - learningRate * g
            bias[j] += velB[j]
        }

        return gradInput
    }

    fun getWeights(): FloatArray = weights.copyOf()
    fun getBias(): FloatArray = bias.copyOf()
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

class DigitClassifier(
    inputSize: Int = 50 * 50,
    hiddenSize: Int = 256,
    numClasses: Int = 10,
    seed: Int = 42
) {
    private val random = Random(seed)
    private val dense1 = DenseLayer(inputSize = inputSize, outputSize = hiddenSize, random = random)
    private val relu = ReLULayer()
    private val dense2 =
        DenseLayer(inputSize = hiddenSize, outputSize = numClasses, random = random)

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
        java.io.DataOutputStream(java.io.BufferedOutputStream(java.io.FileOutputStream(file)))
            .use { out ->
                out.writeInt(1)
                for (w in dense1.getWeights()) out.writeFloat(w)
                for (b in dense1.getBias()) out.writeFloat(b)
                for (w in dense2.getWeights()) out.writeFloat(w)
                for (b in dense2.getBias()) out.writeFloat(b)
            }
    }
}

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
                imagesArr[i] =
                    resizeAndNormalize(srcImage, srcRows, srcCols, outputRows, outputCols)
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
        val srcYf = (y + 0.5f) * srcRows / outRows - 0.5f
        val y0 = srcYf.toInt().coerceIn(0, srcRows - 1)
        val y1 = (y0 + 1).coerceIn(0, srcRows - 1)
        val wy = srcYf - y0
        for (x in 0 until outCols) {
            val srcXf = (x + 0.5f) * srcCols / outCols - 0.5f
            val x0 = srcXf.toInt().coerceIn(0, srcCols - 1)
            val x1 = (x0 + 1).coerceIn(0, srcCols - 1)
            val wx = srcXf - x0
            val top = src[y0 * srcCols + x0] * (1f - wx) + src[y0 * srcCols + x1] * wx
            val bottom = src[y1 * srcCols + x0] * (1f - wx) + src[y1 * srcCols + x1] * wx
            out[y * outCols + x] = (top * (1f - wy) + bottom * wy) / 255f
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
val testImages = File(mnistDir, "t10k-images-idx3-ubyte")
val testLabels = File(mnistDir, "t10k-labels-idx1-ubyte")

for (f in listOf(trainImages, trainLabels, testImages, testLabels)) {
    if (!f.exists()) {
        System.err.println("ERROR: File not found: ${f.absolutePath}")
        System.exit(1)
    }
}

println("Loading training data")
val train = loadMnist(trainImages, trainLabels)

println("Loading test data")
val test = loadMnist(testImages, testLabels)

val model = DigitClassifier(inputSize = 50 * 50, hiddenSize = 256, numClasses = 10, seed = 42)

val epochs = 20
val batchSize = 32
val learningRate = 0.01f
val rng = Random(42)
val n = train.images.size
val indices = IntArray(n) { it }

val totalBatches = (n + batchSize - 1) / batchSize
val progressStep = 50

println("\nStarting training: $epochs epochs, batchSize=$batchSize, lr=$learningRate")
println("Total batches per epoch: $totalBatches")
println("=".repeat(60))

val trainingStartMs = System.currentTimeMillis()

for (epoch in 1..epochs) {
    MathOps.shuffleInPlace(indices, rng)
    var lossSum = 0f
    var steps = 0
    var start = 0
    val epochStartMs = System.currentTimeMillis()

    while (start < n) {
        val end = minOf(start + batchSize, n)
        val bs = end - start
        val batchX = Array(bs) { i -> train.images[indices[start + i]] }
        val batchY = IntArray(bs) { i -> train.labels[indices[start + i]] }
        lossSum += model.trainOnBatch(batchX, batchY, learningRate)
        steps++
        start = end

        if (steps % progressStep == 0 || steps == totalBatches) {
            val pct = steps * 100 / totalBatches
            val avgLoss = lossSum / steps
            print(
                "\rEpoch $epoch/$epochs  [batch $steps/$totalBatches  $pct%%]  loss=%.4f".format(
                    avgLoss
                )
            )
            System.out.flush()
        }
    }

    val epochSec = (System.currentTimeMillis() - epochStartMs) / 1000.0
    val trainAcc = evaluateAccuracy(model, train.images, train.labels)
    val testAcc = evaluateAccuracy(model, test.images, test.labels)
    println(
        "\rEpoch %d/%d  loss=%.4f  train_acc=%.4f  test_acc=%.4f  (%.1fs)"
            .format(epoch, epochs, lossSum / steps, trainAcc, testAcc, epochSec)
    )
}

val totalSec = (System.currentTimeMillis() - trainingStartMs) / 1000.0
println("=".repeat(60))
println("Training complete! Total time: %.1fs".format(totalSec))

val desktopDir = File(System.getProperty("user.home"), "Desktop")
val modelFile = File(if (desktopDir.isDirectory) desktopDir else projectRoot, "digit_model.bin")
model.saveToFile(modelFile)
println("Model saved to: ${modelFile.absolutePath}")
