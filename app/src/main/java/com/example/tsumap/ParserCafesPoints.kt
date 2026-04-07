package com.example.tsumap

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point

object ParserCafesPoints {

    fun extractFoodPoints(
        bitmap: Bitmap,
        gridWidth: Int,
        gridHeight: Int
    ): List<Point> {

        val rawPoints = mutableListOf<Point>()


        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {

                val pixel = bitmap.getPixel(x, y)

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                if (r > 200 && g < 100 && b < 100) {
                    rawPoints.add(Point(x, y))
                }
            }
        }


        val visited = mutableSetOf<Point>()
        val clusters = mutableListOf<List<Point>>()

        val directions = listOf(
            1 to 0, -1 to 0,
            0 to 1, 0 to -1
        )

        for (point in rawPoints) {

            if (point in visited) continue

            val cluster = mutableListOf<Point>()
            val queue = ArrayDeque<Point>()

            queue.add(point)
            visited.add(point)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                cluster.add(current)

                for ((dx, dy) in directions) {
                    val nx = current.x + dx
                    val ny = current.y + dy
                    val neighbor = Point(nx, ny)

                    if (neighbor in rawPoints && neighbor !in visited) {
                        visited.add(neighbor)
                        queue.add(neighbor)
                    }
                }
            }

            clusters.add(cluster)
        }

        val centers = clusters.map { cluster ->

            val avgX = cluster.map { it.x }.average()
            val avgY = cluster.map { it.y }.average()

            Point(avgX.toInt(), avgY.toInt())
        }


        val result = centers.map {
            val gridX = it.x * gridWidth / bitmap.width
            val gridY = it.y * gridHeight / bitmap.height
            Point(gridX, gridY)
        }

        return result
    }
}