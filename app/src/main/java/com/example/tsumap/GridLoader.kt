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