package com.example.tsumap

import kotlin.math.log2


sealed class DecisionNode {
    data class Split(
        val feature: String,
        val children: Map<String, DecisionNode>,
        val majorityLabel: String
    ) : DecisionNode()

    data class Leaf(
        val prediction: String
    ) : DecisionNode()
}


fun entropy(rows: List<TrainingRow>): Double {
    if (rows.isEmpty()) return 0.0

    val total = rows.size.toDouble()
    val counts = rows.groupingBy { it.label }.eachCount()

    var h = 0.0
    for ((_, count) in counts) {
        val p = count / total
        if (p > 0.0) {
            h -= p * log2(p)
        }
    }
    return h
}

fun informationGain(rows: List<TrainingRow>, feature: String): Double {
    if (rows.isEmpty()) return 0.0

    val baseEntropy = entropy(rows)
    val total = rows.size.toDouble()

    val groups = rows.groupBy { it.features[feature].orEmpty() }

    var weightedEntropy = 0.0
    for ((_, subset) in groups) {
        val weight = subset.size / total
        weightedEntropy += weight * entropy(subset)
    }

    return baseEntropy - weightedEntropy
}

fun majorityLabel(rows: List<TrainingRow>): String {
    return rows
        .groupingBy { it.label }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: "unknown"
}

fun allSameLabel(rows: List<TrainingRow>): Boolean {
    if (rows.isEmpty()) return true
    val first = rows.first().label
    return rows.all { it.label == first }
}


fun trainDecisionTree(
    rows: List<TrainingRow>,
    features: List<String>,
    parentMajority: String? = null,
    forceRootFeature: String? = null
): DecisionNode {
    if (rows.isEmpty()) {
        return DecisionNode.Leaf(parentMajority ?: "unknown")
    }

    if (allSameLabel(rows)) {
        return DecisionNode.Leaf(rows.first().label)
    }

    if (features.isEmpty()) {
        return DecisionNode.Leaf(majorityLabel(rows))
    }

    val currentMajority = majorityLabel(rows)

    val bestFeature = if (forceRootFeature != null && forceRootFeature in features) {
        forceRootFeature
    } else {
        features.maxByOrNull { informationGain(rows, it) }
            ?: return DecisionNode.Leaf(currentMajority)
    }

    val groups = rows.groupBy { it.features[bestFeature].orEmpty() }
    if (groups.size <= 1){
        return DecisionNode.Leaf(currentMajority)
    }


    val remainingFeatures = features.filter { it != bestFeature }
    val children = groups.mapValues { (_, subset) ->
        trainDecisionTree(
            rows = subset,
            features = remainingFeatures,
            parentMajority = currentMajority,
            forceRootFeature = null
        )
    }

    return DecisionNode.Split(
        feature = bestFeature,
        children = children,
        majorityLabel = currentMajority
    )
}


