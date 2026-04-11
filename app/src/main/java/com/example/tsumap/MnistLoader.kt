package com.example.tsumap

import android.content.Context
import java.io.BufferedInputStream
import java.io.DataInputStream

data class MnistDataset(
    val images: Array<IntArray>,
    val labels: IntArray,
    val rows: Int,
    val cols: Int
)

object MnistLoader {

    fun loadTrain(context: Context): MnistDataset {
        return load(
            context,
            imagesPath = "mnist/train-images-idx3-ubyte",
            labelsPath = "mnist/train-labels-idx1-ubyte"
        )
    }

    fun loadTest(context: Context): MnistDataset {
        return load(
            context,
            imagesPath = "mnist/t10k-images-idx3-ubyte",
            labelsPath = "mnist/t10k-labels-idx1-ubyte"
        )
    }

    fun load(context: Context, imagesPath: String, labelsPath: String): MnistDataset {
        val imagesInput = DataInputStream(BufferedInputStream(context.assets.open(imagesPath)))
        val labelsInput = DataInputStream(BufferedInputStream(context.assets.open(labelsPath)))

        imagesInput.use { img ->
            labelsInput.use { lbl ->
                val imageMagic = img.readInt()
                require(imageMagic == 2051) { "Invalid image magic: $imageMagic (expected 2051)" }

                val numImages = img.readInt()
                val rows = img.readInt()
                val cols = img.readInt()

                val labelMagic = lbl.readInt()
                require(labelMagic == 2049) { "Invalid label magic: $labelMagic (expected 2049)" }

                val numLabels = lbl.readInt()
                require(numImages == numLabels) {
                    "Images count ($numImages) != Labels count ($numLabels)"
                }
                require(rows == 28 && cols == 28) {
                    "Expected 28x28, got ${rows}x${cols}"
                }

                val imageSize = rows * cols
                val images = Array(numImages) { IntArray(imageSize) }
                val labels = IntArray(numImages)

                for (i in 0 until numImages) {
                    for (p in 0 until imageSize) {
                        images[i][p] = img.readUnsignedByte() // 0..255
                    }
                    labels[i] = lbl.readUnsignedByte() // 0..9
                }

                return MnistDataset(images, labels, rows, cols)
            }
        }
    }
}