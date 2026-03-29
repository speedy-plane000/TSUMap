package com.example.tsumap

import androidx.compose.foundation.pager.PagerSnapDistance
import kotlin.time.Instant

enum class DistanceMode {
    EUCLIDEAN,
    ASTAR
}

data class Point(
    val x: Int,
    val y: Int
)

data class Cluster(
    val points: MutableList<Point>,
    var centerX: Float,
    var centerY: Float
)

fun KMeans(
    points: List<Point>,
    centers: List<Pair<Float, Float>>,
    grid: Array<IntArray>,
    mode: DistanceMode,
    iterations: Int = 10
): List<Cluster>{

    var currentCenters = centers.toList()

    var clusters: List<Cluster> = emptyList()

    repeat(iterations) {

        clusters = List(centers.size) {
            Cluster(mutableListOf(), 0f, 0f)
        }
        for (p in points) {
            val nearestIndex = centers.indices.minByOrNull { i ->
                val centerPoint = Point(
                    centers[i].first.toInt(),
                    centers[i].second.toInt()
                )
                getDistance(mode, grid, p, centerPoint)
            }!!
            clusters[nearestIndex].points.add(p)
        }

        clusters.forEach { cluster ->
            val avgX = cluster.points.map { it.x }.average().toFloat()
            val avgY = cluster.points.map { it.y }.average().toFloat()

            cluster.centerX = avgX
            cluster.centerY = avgY
        }
        currentCenters = clusters.map { it.centerX to it.centerY }
    }
    return clusters
}

fun euclidean(a: Point, b: Point): Double {
    return kotlin.math.sqrt(
        ((a.x - b.x).toDouble() * (a.x - b.x)) + ((a.y - b.y).toDouble() * (a.y - b.y))
    )
}

fun aStarDistanceForCluster(
    grid: Array<IntArray>,
    a: Point,
    b: Point
): Double {
    if (grid[a.y][a.x] != 1 || grid[b.y][b.x] != 1) return Double.MAX_VALUE
    val path = aStar(grid, a.x to a.y, b.x to b.y)
    return if (path.isEmpty()) Double.MAX_VALUE else path.size.toDouble()
}

fun getDistance(
    mode: DistanceMode,
    grid: Array<IntArray>,
    a: Point,
    b: Point
): Double {
    return when (mode) {
        DistanceMode.EUCLIDEAN -> euclidean(a, b)
        DistanceMode.ASTAR -> aStarDistanceForCluster(grid, a, b)
    }
}