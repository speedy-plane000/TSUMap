package com.example.tsumap

import kotlin.math.pow

fun antColonyPath(
    grid: Array<IntArray>,
    points: List<Point>,
    iterations: Int = 50,
    ants: Int = 20,
    alpha: Double = 1.0,
    beta: Double = 3.0,
    evaporation: Double = 0.5
): List<Pair<Int, Int>> {

    val n = points.size

    val dist = Array(n) { DoubleArray(n) }
    for (i in 0 until n) {
        for (j in 0 until n) {
            if (i == j) continue
            val d = aStarDistanceForCluster(grid, points[i], points[j])
            dist[i][j] = if (d == Double.MAX_VALUE) 1e6 else d
        }
    }

    val pheromone = Array(n) { DoubleArray(n) { 1.0 } }

    var bestPath = listOf<Int>()
    var bestLength = Double.MAX_VALUE

    repeat(iterations) {
        repeat(ants) {

            val visited = mutableSetOf<Int>()
            val path = mutableListOf<Int>()

            var current = 0
            visited.add(current)
            path.add(current)

            while (visited.size < n) {
                val probs = DoubleArray(n)
                var sum = 0.0

                for (j in 0 until n) {
                    if (j !in visited) {
                        val p = pheromone[current][j].pow(alpha) *
                                (1.0 / (dist[current][j] + 1e-6)).pow(beta)
                        probs[j] = p
                        sum += p
                    }
                }

                val r = Math.random() * sum
                var acc = 0.0
                var next = 0

                for (j in 0 until n) {
                    if (j !in visited) {
                        acc += probs[j]
                        if (acc >= r) {
                            next = j
                            break
                        }
                    }
                }

                visited.add(next)
                path.add(next)
                current = next
            }

            var length = 0.0
            for (i in 0 until path.size - 1) {
                length += dist[path[i]][path[i + 1]]
            }

            if (length < bestLength) {
                bestLength = length
                bestPath = path
            }
        }

        for (i in 0 until n) {
            for (j in 0 until n) {
                pheromone[i][j] *= (1 - evaporation)
            }
        }

        for (i in 0 until bestPath.size - 1) {
            val a = bestPath[i]
            val b = bestPath[i + 1]
            pheromone[a][b] += 1.0 / bestLength
        }
    }

    if (bestPath.isEmpty()) return emptyList()

    val finalPath = mutableListOf<Pair<Int, Int>>()

    for (i in 0 until bestPath.size - 1) {
        val segment = aStar(
            grid,
            points[bestPath[i]].x to points[bestPath[i]].y,
            points[bestPath[i + 1]].x to points[bestPath[i + 1]].y
        )
        finalPath.addAll(segment.dropLast(1))
    }

    val last = bestPath.last()
    val first = bestPath.first()

    val returnSegment = aStar(
        grid,
        points[last].x to points[last].y,
        points[first].x to points[first].y
    )

    if (returnSegment.isNotEmpty()) {
        finalPath.addAll(returnSegment.dropLast(1))
    }

    finalPath.add(
        points[first].x to points[first].y
    )

    return finalPath
}

