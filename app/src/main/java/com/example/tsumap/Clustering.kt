package com.example.tsumap

import kotlin.math.abs
import kotlin.math.sqrt


fun convexHull(points: List<Point>): List<Point> {
    if (points.size <= 2) return points.distinct()

    fun cross(o: Point, a: Point, b: Point): Long =
        (a.x - o.x).toLong() * (b.y - o.y) - (a.y - o.y).toLong() * (b.x - o.x)

    val pts = points.distinct().sortedWith(compareBy<Point> { it.x }.thenBy { it.y })

    if (pts.size <= 2) return pts

    val lower = mutableListOf<Point>()

    for (p in pts) {
        while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], p) <= 0) {
            lower.removeAt(lower.size - 1)
        }
        lower.add(p)
    }

    val upper = mutableListOf<Point>()

    for (p in pts.asReversed()) {
        while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], p) <= 0) {
            upper.removeAt(upper.size - 1)
        }
        upper.add(p)
    }

    lower.removeAt(lower.size - 1)
    upper.removeAt(upper.size - 1)

    return lower + upper
}

enum class DistanceMode {
    EUCLIDEAN,
    ASTAR
}

data class Point(
    val x: Int,
    val y: Int
)

data class Landmark(
    val name: String,
    val point: Point,
    var selected: Boolean = false
)

data class Cluster(
    val points: MutableList<Point> = mutableListOf(),
    var centerX: Float = 0f,
    var centerY: Float = 0f
)

fun kMeans(
    points: List<Point>,
    centers: List<Pair<Float, Float>>,
    grid: Array<IntArray>,
    mode: DistanceMode,
    iterations: Int = 30
): List<Cluster> {
    if (points.isEmpty() || centers.isEmpty()) return emptyList()

    val preparedPoints =
        if (mode == DistanceMode.ASTAR) snapPointsToRoad(grid, points) else points

    var currentCenters =
        if (mode == DistanceMode.ASTAR) snapCentersToRoad(grid, centers) else centers

    val k = currentCenters.size
    val clusters = List(k) { Cluster() }

    fun centerAsPoint(i: Int): Point {
        return Point(
            currentCenters[i].first.toInt(),
            currentCenters[i].second.toInt()
        )
    }



    repeat(iterations) {
        clusters.forEach { it.points.clear() }

        for (p in preparedPoints) {
            val nearestIndex = (0 until k).minByOrNull { i ->
                val c = centerAsPoint(i)
                val d = when (mode) {
                    DistanceMode.EUCLIDEAN -> euclidean(p, c)
                    DistanceMode.ASTAR -> aStarDistanceForCluster(grid,p, c)
                }
                if (d == Double.MAX_VALUE) Double.POSITIVE_INFINITY else d
            } ?: 0

            clusters[nearestIndex].points.add(p)
        }

        val newCenters = MutableList(k) { 0f to 0f }

        for (i in 0 until k) {
            val pts = clusters[i].points

            if (pts.isNotEmpty()) {
                val meanX = pts.sumOf { it.x }.toFloat() / pts.size
                val meanY = pts.sumOf { it.y }.toFloat() / pts.size
                newCenters[i] = meanX to meanY
            } else {
                val farthestPoint = preparedPoints.maxByOrNull { p ->
                    val nearestDist = (0 until k).minOf { j ->
                        val c = centerAsPoint(j)
                        val d = when (mode) {
                            DistanceMode.EUCLIDEAN -> euclidean(p, c)
                            DistanceMode.ASTAR -> aStarDistanceForCluster(grid,p, c)
                        }
                        if (d == Double.MAX_VALUE) Double.POSITIVE_INFINITY else d
                    }
                    nearestDist
                } ?: preparedPoints.first()

                newCenters[i] = farthestPoint.x.toFloat() to farthestPoint.y.toFloat()
            }
        }

        currentCenters =
            if (mode == DistanceMode.ASTAR) snapCentersToRoad(grid, newCenters) else newCenters
    }

    clusters.forEachIndexed { i, cl ->
        cl.centerX = currentCenters[i].first
        cl.centerY = currentCenters[i].second
    }

    return clusters
}

fun euclidean(a: Point, b: Point): Double {
    val dx = (a.x - b.x).toDouble()
    val dy = (a.y - b.y).toDouble()
    return sqrt(dx * dx + dy * dy)
}

fun aStarDistanceForCluster(
    grid: Array<IntArray>,
    a: Point,
    b: Point
): Double {
    if (!isWalkable(grid, a.x, a.y) || !isWalkable(grid, b.x, b.y)) {
        return Double.MAX_VALUE
    }

    val dist = aStarDistance(grid, a.x to a.y, b.x to b.y)
    return if (dist < 0) Double.MAX_VALUE else dist.toDouble()
}



private fun isWalkable(
    grid: Array<IntArray>,
    x: Int,
    y: Int
): Boolean {
    return y in grid.indices && x in grid[0].indices && grid[y][x] == 1
}



fun snapPointsToRoad(
    grid: Array<IntArray>,
    points: List<Point>
): List<Point> {
    return points.map { p ->
        val x = p.x.coerceIn(0, grid[0].size - 1)
        val y = p.y.coerceIn(0, grid.size - 1)
        val snapped = findNearestRoad(grid, x, y)

        if (snapped != null) {
            Point(snapped.first, snapped.second)
        } else {
            Point(x, y)
        }
    }
}

fun snapCentersToRoad(
    grid: Array<IntArray>,
    centers: List<Pair<Float, Float>>
): List<Pair<Float, Float>> {
    return centers.map { (cx, cy) ->
        val x = cx.toInt().coerceIn(0, grid[0].size - 1)
        val y = cy.toInt().coerceIn(0, grid.size - 1)
        val snapped = findNearestRoad(grid, x, y)

        if (snapped != null) {
            snapped.first.toFloat() to snapped.second.toFloat()
        } else {
            x.toFloat() to y.toFloat()
        }
    }
}