package com.example.tsumap

import android.content.Context
import androidx.compose.ui.graphics.colorspace.Connector



data class TrainingRow(
    val features: Map<String, String>,
    val label: String
)

fun loadTrainingRows(
    context: Context,
    fileName: String = "data.csv",
): List<TrainingRow>{
    val lines = context.assets.open(fileName)
        .bufferedReader()
        .readLines()
        .filter { it.isNotBlank() }

    if (lines.size < 2) return emptyList()

    val headers = lines.first().split(",")
    val featureNames = headers.dropLast(1)

    return lines.drop(1).mapNotNull { line ->
        val cols = line.split(",")
        if (cols.size != headers.size) return@mapNotNull    null
        val features = featureNames.indices.associate { i ->
            featureNames[i].trim() to cols[i].trim()
        }

        TrainingRow(
            features = features,
            label = cols.last().trim()
        )
    }
}

