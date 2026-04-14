package com.example.tsumap

import android.content.Context
import kotlin.random.Random

data class TrainingConfig(
    val epochs: Int = 5,
    val batchSize: Int = 32,
    val learningRate: Float = 0.01f,
    val shuffleEachEpoch: Boolean = true,
    val seed: Int = 42
)

data class EpochMetrics(
    val epoch: Int,
    val trainLoss: Float,
    val trainAccuracy: Float,
    val testAccuracy: Float? = null
)

class Trainer(private val config: TrainingConfig) {

    fun fit(
        model: DigitClassifier,
        train: MnistTrainingDataset,
        test: MnistTrainingDataset? = null,
        onEpochEnd: ((EpochMetrics) -> Unit)? = null
    ): List<EpochMetrics> {
        val random = Random(config.seed)
        val n = train.images.size
        require(n == train.labels.size) { "Train images/labels size mismatch" }

        val indices = IntArray(n) { it }
        val history = mutableListOf<EpochMetrics>()

        repeat(config.epochs) { epochIdx ->
            if (config.shuffleEachEpoch) {
                MathOps.shuffleInPlace(indices, random)
            }

            var lossSum = 0f
            var steps = 0
            var start = 0

            while (start < n) {
                val end = minOf(start + config.batchSize, n)
                val batchSize = end - start

                val batchX = Array(batchSize) { i ->
                    train.images[indices[start + i]]
                }
                val batchY = IntArray(batchSize) { i ->
                    train.labels[indices[start + i]]
                }

                val loss = model.trainOnBatch(batchX, batchY, config.learningRate)
                lossSum += loss
                steps++
                start = end
            }

            val trainAcc = evaluateAccuracy(model, train.images, train.labels)
            val testAcc = test?.let { evaluateAccuracy(model, it.images, it.labels) }

            val metrics = EpochMetrics(
                epoch = epochIdx + 1,
                trainLoss = if (steps > 0) lossSum / steps else 0f,
                trainAccuracy = trainAcc,
                testAccuracy = testAcc
            )

            history.add(metrics)
            onEpochEnd?.invoke(metrics)
        }

        return history
    }

    private fun evaluateAccuracy(
        model: DigitClassifier,
        images: Array<FloatArray>,
        labels: IntArray
    ): Float {
        val preds = model.predictBatch(images)
        var correct = 0
        for (i in preds.indices) {
            if (preds[i] == labels[i]) correct++
        }
        return correct.toFloat() / preds.size
    }
}

object TrainDigitModelUseCase {
    fun run(context: Context): Pair<DigitClassifier, List<EpochMetrics>> {
        val train = MnistLoader.loadTrainResized50(context, limit = 10_000)
        val test = MnistLoader.loadTestResized50(context, limit = 2_000)

        val model = DigitClassifier(
            inputSize = 50 * 50,
            hiddenSize = 128,
            numClasses = 10,
            seed = 42
        )

        val trainer = Trainer(
            TrainingConfig(
                epochs = 5,
                batchSize = 32,
                learningRate = 0.01f,
                shuffleEachEpoch = true,
                seed = 42
            )
        )

        val history = trainer.fit(model, train, test)
        return model to history
    }
}