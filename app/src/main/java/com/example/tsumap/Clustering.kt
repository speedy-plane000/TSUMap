package com.example.tsumap

import kotlin.math.abs
import kotlin.math.sqrt

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
    val distanceCache = HashMap<Pair<Point, Point>, Double>()

    fun centerAsPoint(i: Int): Point {
        return Point(
            currentCenters[i].first.toInt(),
            currentCenters[i].second.toInt()
        )
    }

    fun cachedAStarDistance(a: Point, b: Point): Double {
        val key = if (a.x < b.x || (a.x == b.x && a.y <= b.y)) a to b else b to a
        return distanceCache.getOrPut(key) { aStarDistanceForCluster(grid, a, b) }
    }

    repeat(iterations) {
        clusters.forEach { it.points.clear() }

        for (p in preparedPoints) {
            val nearestIndex = (0 until k).minByOrNull { i ->
                val c = centerAsPoint(i)
                val d = when (mode) {
                    DistanceMode.EUCLIDEAN -> euclidean(p, c)
                    DistanceMode.ASTAR -> cachedAStarDistance(p, c)
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
                            DistanceMode.ASTAR -> cachedAStarDistance(p, c)
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

    val len = aStarLength(grid, a.x to a.y, b.x to b.y)
    return if (len < 0) Double.MAX_VALUE else len.toDouble()
}

fun getDistance(
    mode: DistanceMode,
    grid: Array<IntArray>,
    a: Point,
    b: Point,
): Double {
    return when (mode) {
        DistanceMode.EUCLIDEAN -> euclidean(a, b)
        DistanceMode.ASTAR -> aStarDistanceForCluster(grid, a, b)
    }
}

private fun isWalkable(
    grid: Array<IntArray>,
    x: Int,
    y: Int
): Boolean {
    return y in grid.indices && x in grid[0].indices && grid[y][x] == 1
}

private data class NodeRec(
    val x: Int,
    val y: Int,
    var g: Int = Int.MAX_VALUE,
    var h: Int = 0
) {
    val f: Int
        get() = g + h
}

fun aStarLength(
    grid: Array<IntArray>,
    start: Pair<Int, Int>,
    end: Pair<Int, Int>
): Int {
    val rows = grid.size
    val cols = grid[0].size

    fun heuristic(x: Int, y: Int): Int {
        return abs(x - end.first) + abs(y - end.second)
    }

    if (!isWalkable(grid, start.first, start.second) ||
        !isWalkable(grid, end.first, end.second)
    ) {
        return -1
    }

    val nodes = Array(rows) { y -> Array(cols) { x -> NodeRec(x, y) } }
    val closed = Array(rows) { BooleanArray(cols) }
    val openSet = mutableListOf<NodeRec>()

    val startNode = nodes[start.second][start.first]
    startNode.g = 0
    startNode.h = heuristic(startNode.x, startNode.y)
    openSet.add(startNode)

    val dirs = arrayOf(
        1 to 0,
        -1 to 0,
        0 to 1,
        0 to -1
    )

    while (openSet.isNotEmpty()) {
        val current = openSet.minByOrNull { it.f } ?: break
        openSet.remove(current)

        if (closed[current.y][current.x]) continue
        closed[current.y][current.x] = true

        if (current.x == end.first && current.y == end.second) {
            return current.g
        }

        for ((dx, dy) in dirs) {
            val nx = current.x + dx
            val ny = current.y + dy

            if (!isWalkable(grid, nx, ny) || closed[ny][nx]) continue

            val neighbor = nodes[ny][nx]
            val newG = current.g + 1

            if (newG < neighbor.g) {
                neighbor.g = newG
                neighbor.h = heuristic(nx, ny)

                if (neighbor !in openSet) {
                    openSet.add(neighbor)
                }
            }
        }
    }

    return -1
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