package com.example.tsumap

data class Node(
    val x: Int,
    val y: Int,
    var g: Int = Int.MAX_VALUE,
    var h: Int = 0,
    var parent: Node? = null
) {
    val f get() = g + h
}

data class AStarStep(
    val current: Pair<Int, Int>?,
    val closedSet: List<Pair<Int, Int>>,
    val path: List<Pair<Int, Int>>
)
fun aStar(
    grid: Array<IntArray>,
    start: Pair<Int, Int>,
    end: Pair<Int, Int>,
    obstacles: Set<Pair<Int, Int>> = emptySet()
): List<Pair<Int, Int>> {

    val rows = grid.size
    val cols = grid[0].size

    fun heuristic(x: Int, y: Int) =
        kotlin.math.abs(x - end.first) + kotlin.math.abs(y - end.second)

    val openSet = mutableListOf<Node>()
    val allNodes = Array(rows) { y ->
        Array(cols) { x -> Node(x, y) }
    }

    val startNode = allNodes[start.second][start.first]
    startNode.g = 0
    startNode.h = heuristic(start.first, start.second)

    openSet.add(startNode)

    val directions = listOf(
        1 to 0, -1 to 0,
        0 to 1, 0 to -1
    )

    while (openSet.isNotEmpty()) {
        val current = openSet.minByOrNull { it.f }!!

        if (current.x == end.first && current.y == end.second) {
            val path = mutableListOf<Pair<Int, Int>>()
            var node: Node? = current
            while (node != null) {
                path.add(node.x to node.y)
                node = node.parent
            }
            return path.reversed()
        }

        openSet.remove(current)

        for ((dx, dy) in directions) {
            val nx = current.x + dx
            val ny = current.y + dy

            if (nx !in 0 until cols || ny !in 0 until rows) continue
            if (grid[ny][nx] != 1 || (nx to ny) in obstacles) continue

            val neighbor = allNodes[ny][nx]
            val newG = current.g + 1

            if (newG < neighbor.g) {
                neighbor.g = newG
                neighbor.h = heuristic(nx, ny)
                neighbor.parent = current

                if (neighbor !in openSet) {
                    openSet.add(neighbor)
                }
            }
        }
    }

    return emptyList()
}

fun aStarWithSteps(
    grid: Array<IntArray>,
    start: Pair<Int, Int>,
    end: Pair<Int, Int>,
    obstacles: Set<Pair<Int, Int>> = emptySet()
): List<AStarStep> {

    val steps = mutableListOf<AStarStep>()

    val openSet = mutableListOf<Node>()
    val closedSet = mutableSetOf<Pair<Int, Int>>()

    val allNodes = Array(grid.size) { y ->
        Array(grid[0].size) { x -> Node(x, y) }
    }

    fun heuristic(x: Int, y: Int) =
        kotlin.math.abs(x - end.first) + kotlin.math.abs(y - end.second)

    val startNode = allNodes[start.second][start.first]
    startNode.g = 0
    startNode.h = heuristic(start.first, start.second)

    openSet.add(startNode)

    val directions = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

    while (openSet.isNotEmpty()) {

        val current = openSet.minByOrNull { it.f }!!

        steps.add(
            AStarStep(
                current = current.x to current.y,
                closedSet = closedSet.toList(),
                path = emptyList()
            )
        )

        if (current.x == end.first && current.y == end.second) {

            val path = mutableListOf<Pair<Int, Int>>()
            var node: Node? = current

            while (node != null) {
                path.add(node.x to node.y)
                node = node.parent
            }

            return steps + AStarStep(
                current = null,
                closedSet = closedSet.toList(),
                path = path.reversed()
            )
        }

        openSet.remove(current)
        closedSet.add(current.x to current.y)

        for ((dx, dy) in directions) {
            val nx = current.x + dx
            val ny = current.y + dy

            if (nx !in 0 until grid[0].size || ny !in 0 until grid.size) continue
            if (grid[ny][nx] != 1 || (nx to ny) in obstacles) continue

            val neighbor = allNodes[ny][nx]
            val newG = current.g + 1

            if (newG < neighbor.g) {
                neighbor.g = newG
                neighbor.h = heuristic(nx, ny)
                neighbor.parent = current

                if (neighbor !in openSet) {
                    openSet.add(neighbor)
                }
            }
        }
    }

    return steps
}