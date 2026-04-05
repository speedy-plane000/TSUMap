package com.example.tsumap

import android.content.Context
import org.json.JSONArray
fun loadGrid(context: Context): Array<IntArray> {
    val input = context.assets.open("grid.json")

    val text = input.bufferedReader().use { it.readText() }

    val jsonArray = JSONArray(text)

    val grid = Array(jsonArray.length()) { IntArray(0) }

    for (i in 0 until jsonArray.length()) {
        val row = jsonArray.getJSONArray(i)
        grid[i] = IntArray(row.length())

        for (j in 0 until row.length()) {
            grid[i][j] = row.getInt(j)
        }
    }

    return grid
}

fun mapLatLngToGrid(
    lat: Double,
    lng: Double,
    grid: Array<IntArray>
): Point {

    val minLat = 56.463717
    val maxLat = 56.47319
    val minLng = 84.940748
    val maxLng = 84.954112

    val rows = grid.size
    val cols = grid[0].size

    val normX = (lng - minLng) / (maxLng - minLng)
    val normY = (maxLat - lat) / (maxLat - minLat)

    val x = (normY * cols).toInt()
    val y = ((1 - normX) * rows).toInt()

    return Point(
        x.coerceIn(0, cols - 1),
        y.coerceIn(0, rows - 1)
    )
}