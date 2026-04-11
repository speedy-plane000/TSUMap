package com.example.tsumap

import android.content.Context
import java.io.BufferedInputStream
import java.io.DataInputStream

data class MnistTrainingDataset(
    val images: Array<FloatArray>,
    val labels: IntArray,
    val rows: Int,
    val cols: Int
)

object MnistLoader {

    fun loadTrainResized50(context: Context, limit: Int? = null): MnistTrainingDataset {
        return loadResized50(
            context,
            imagesPath = "mnist/train-images-idx3-ubyte",
            labelsPath = "mnist/train-labels-idx1-ubyte",
            limit = limit
        )
    }

    fun loadTestResized50(context: Context, limit: Int? = null): MnistTrainingDataset {
        return loadResized50(
            context,
            imagesPath = "mnist/t10k-images-idx3-ubyte",
            labelsPath = "mnist/t10k-labels-idx1-ubyte",
            limit = limit
        )
    }

    private fun loadResized50(
        context: Context,
        imagesPath: String,
        labelsPath: String,
        limit: Int? = null
    ): MnistTrainingDataset {
        val imagesInput = DataInputStream(BufferedInputStream(context.assets.open(imagesPath)))
        val labelsInput = DataInputStream(BufferedInputStream(context.assets.open(labelsPath)))

        imagesInput.use { img ->
            labelsInput.use { lbl ->
                val imageMagic = img.readInt()
                require(imageMagic == 2051) { "Invalid image magic: $imageMagic (expected 2051)" }

                val numImages = img.readInt()
                val srcRows = img.readInt()
                val srcCols = img.readInt()

                val labelMagic = lbl.readInt()
                require(labelMagic == 2049) { "Invalid label magic: $labelMagic (expected 2049)" }

                val numLabels = lbl.readInt()
                require(numImages == numLabels) {
                    "Images count ($numImages) != Labels count ($numLabels)"
                }
                require(srcRows == 28 && srcCols == 28) {
                    "Expected 28x28, got ${srcRows}x${srcCols}"
                }

                val outputRows = 50
                val outputCols = 50
                val requestedCount = limit?.coerceIn(1, numImages) ?: numImages

                val images = Array(requestedCount) { FloatArray(outputRows * outputCols) }
                val labels = IntArray(requestedCount)

                val srcImage = IntArray(srcRows * srcCols)

                for (i in 0 until requestedCount) {
                    for (p in srcImage.indices) {
                        srcImage[p] = img.readUnsignedByte()
                    }
                    labels[i] = lbl.readUnsignedByte()

                    images[i] = resizeNearestAndNormalize(
                        src = srcImage,
                        srcRows = srcRows,
                        srcCols = srcCols,
                        outRows = outputRows,
                        outCols = outputCols
                    )
                }

                return MnistTrainingDataset(
                    images = images,
                    labels = labels,
                    rows = outputRows,
                    cols = outputCols
                )
            }
        }
    }
    private fun resizeNearestAndNormalize(
        src: IntArray,
        srcRows: Int,
        srcCols: Int,
        outRows: Int,
        outCols: Int
    ): FloatArray {
        val out = FloatArray(outRows * outCols)

        for (y in 0 until outRows) {
            val srcY = ((y.toFloat() / outRows) * srcRows).toInt().coerceIn(0, srcRows - 1)
            for (x in 0 until outCols) {
                val srcX = ((x.toFloat() / outCols) * srcCols).toInt().coerceIn(0, srcCols - 1)
                val srcValue = src[srcY * srcCols + srcX]
                out[y * outCols + x] = srcValue / 255f
            }
        }

        return out
    }
}