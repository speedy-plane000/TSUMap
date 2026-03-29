package com.example.tsumap

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.compose.material3.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import com.example.tsumap.ui.theme.TSUMapTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.ShortcutInfoCompat
import com.example.tsumap.ui.theme.TsuBlue
import com.example.tsumap.ui.theme.TsuBlueLight
import com.example.tsumap.ui.theme.TsuWhite
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.collections.plusAssign
import kotlin.times
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TSUMapTheme {
                MainMapScreen()
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MainMapScreen() {

    val context = LocalContext.current

    val grid = remember {
        loadGrid(context)
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var path by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var showRoads by remember { mutableStateOf(false) }
    var startPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var endPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectionMode by remember { mutableStateOf<String?>(null) }
    var clusters by remember { mutableStateOf<List<Cluster>>(emptyList()) }
    var differentPoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var clusterMode by remember { mutableStateOf(false) }
    var centers by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var distanceMode by remember { mutableStateOf(DistanceMode.EUCLIDEAN) }
    val initialCentersEuclid = listOf(
        Point(91, 136),
        Point(70, 0),
        Point(111, 68)
    ).map { it.x.toFloat() to it.y.toFloat() }

    val initialCentersAStar = listOf(
        Point(91, 136),
        Point(70, 0),
        Point(99, 52)
    ).map { it.x.toFloat() to it.y.toFloat() }


    val minScale = 1f
    val maxScale = 4f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(TsuWhite)
    ) {
        val density = LocalDensity.current

        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = constraints.maxHeight.toFloat()

        val imageWidth = 686f
        val imageHeight = 563f

        val imgRatio = imageWidth / imageHeight
        val boxRatio = boxWidth / boxHeight

        val actualVisualWidth = if (imgRatio > boxRatio) boxWidth else boxHeight * imgRatio
        val actualVisualHeight = if (imgRatio > boxRatio) boxWidth / imgRatio else boxHeight

        val startX = (boxWidth - actualVisualWidth) / 2f
        val startY = (boxHeight - actualVisualHeight) / 2f

        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->

            val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)

            val maxX = maxOf(0f, (actualVisualWidth * newScale - boxWidth) / 2)
            val maxY = maxOf(0f, (actualVisualHeight * newScale - boxHeight) / 2)

            val newOffset = offset + panChange

            offset = Offset(
                x = newOffset.x.coerceIn(-maxX, maxX),
                y = newOffset.y.coerceIn(-maxY, maxY)
            )

            scale = newScale
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .transformable(transformableState)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->

                            if (selectionMode == null) return@detectTapGestures


                            val tapOnImageX = tapOffset.x - startX
                            val tapOnImageY = tapOffset.y - startY

                            if (tapOnImageX < 0 || tapOnImageY < 0 ||
                                tapOnImageX > actualVisualWidth || tapOnImageY > actualVisualHeight
                            ) {
                                return@detectTapGestures
                            }

                            val rows = grid.size
                            val cols = grid[0].size

                            val cellX = (tapOnImageX / actualVisualWidth * cols).toInt()
                                .coerceIn(0, cols - 1)
                            val cellY = (tapOnImageY / actualVisualHeight * rows).toInt()
                                .coerceIn(0, rows - 1)
                            println("DEBUG_TAG: Clicked at X: $cellX, Y: $cellY")
                            var finalX = cellX
                            var finalY = cellY

                            val cellValue = grid[cellY][cellX]

                            if (cellValue != 1) {
                                val nearest = findNearestRoad(grid, cellX, cellY)
                                if (nearest != null) {
                                    finalX = nearest.first
                                    finalY = nearest.second
                                } else return@detectTapGestures
                            }

                            if (selectionMode == "start") {
                                startPoint = finalX to finalY
                            } else if (selectionMode == "end") {
                                endPoint = finalX to finalY
                            }

                            if (startPoint != null && endPoint != null) {
                                path = aStar(grid, startPoint!!, endPoint!!)
                            }
                        }
                    }
            ) {

                Image(
                    painter = painterResource(id = R.drawable.tsu_map),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                if (showRoads) {
                    RoadsGridOverlay(
                        grid = grid,
                        imageWidth = actualVisualWidth,
                        imageHeight = actualVisualHeight,
                        startX = startX,
                        startY = startY
                    )
                }

                if (path.isNotEmpty() || clusters.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {

                        val cellWidth = actualVisualWidth / grid[0].size
                        val cellHeight = actualVisualHeight / grid.size

                        if (path.isNotEmpty()) {
                            for (i in 0 until path.size - 1) {
                                val (x1, y1) = path[i]
                                val (x2, y2) = path[i + 1]

                                val px1 = startX + (x1 + 0.5f) * cellWidth
                                val py1 = startY + (y1 + 0.5f) * cellHeight

                                val px2 = startX + (x2 + 0.5f) * cellWidth
                                val py2 = startY + (y2 + 0.5f) * cellHeight

                                drawLine(
                                    color = TsuBlue,
                                    start = Offset(px1, py1),
                                    end = Offset(px2, py2),
                                    strokeWidth = 6f
                                )
                            }
                        }
                        if (clusters.isNotEmpty()) {
                            val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Magenta)

                            clusters.forEachIndexed { index, cluster ->
                                val color = colors[index % colors.size]

                                cluster.points.forEach { point ->
                                    val px =
                                        startX + (point.x + 0.5f) / grid[0].size * actualVisualWidth
                                    val py =
                                        startY + (point.y + 0.5f) / grid.size * actualVisualHeight

                                    drawCircle(
                                        color = color,
                                        radius = 16f,
                                        center = Offset(px, py)
                                    )
                                }
                            }

                            differentPoints.forEach { point ->

                                val px =
                                    startX + (point.x + 0.5f) / grid[0].size * actualVisualWidth
                                val py = startY + (point.y + 0.5f) / grid.size * actualVisualHeight

                                drawCircle(
                                    color = Color.Yellow,
                                    radius = 12f,
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }
                }

                startPoint?.let { (x, y) ->

                    val px = startX + (x + 0.5f) / grid[0].size * actualVisualWidth
                    val py = startY + (y + 0.5f) / grid.size * actualVisualHeight

                    val dotSize = 12.dp
                    val dotRadiusPx = with(density) { (dotSize / 2).toPx() }

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (px - dotRadiusPx).toInt(),
                                    (py - dotRadiusPx).toInt()
                                )
                            }
                            .size(16.dp)
                            .background(TsuWhite, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(TsuBlue, CircleShape)
                        )
                    }
                }

                endPoint?.let { (x, y) ->

                    val px = startX + (x + 0.5f) / grid[0].size * actualVisualWidth
                    val py = startY + (y + 0.5f) / grid.size * actualVisualHeight

                    val dotSize = 12.dp
                    val dotRadiusPx = with(density) { (dotSize / 2).toPx() }

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (px - dotRadiusPx).toInt(),
                                    (py - dotRadiusPx).toInt()
                                )
                            }
                            .size(16.dp)
                            .background(TsuBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(TsuWhite, CircleShape)
                        )
                    }
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (!clusterMode) {

                item {
                    Button(
                        onClick = { showRoads = !showRoads },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Показать дороги")
                    }
                }

                item {
                    Button(
                        onClick = {
                            clusterMode = true

                            val points = listOf(
                                Point(91, 136),//nizcenter
                                Point(5, 123),
                                Point(14, 1),
                                Point(70, 0),//verxcenter
                                Point(149, 142),
                                Point(161, 34),
                                Point(150, 8),
                                Point(111, 68),//sercentr
                                Point(116, 70),
                                Point(114, 71),
                                Point(99, 52),
                                Point(142, 104),
                                Point(151, 127),
                            )

                            centers = points.shuffled().take(3).map {
                                Pair(it.x.toFloat(), it.y.toFloat())
                            }

                            clusters = KMeans(points, centers, grid, DistanceMode.EUCLIDEAN)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Кластеры")
                    }
                }

                item {
                    Button(
                        onClick = { selectionMode = "start" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Старт")
                    }
                }

                item {
                    Button(
                        onClick = { selectionMode = "end" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Финиш")
                    }
                }

            } else {


                item {
                    Button(
                        onClick = {
                            distanceMode = DistanceMode.ASTAR
                            centers = initialCentersAStar
                            clusters = KMeans(
                                clusters.flatMap { it.points },
                                centers,
                                grid,
                                distanceMode
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("A*")
                    }
                }

                item {
                    Button(
                        onClick = {
                            distanceMode = DistanceMode.EUCLIDEAN
                            centers = initialCentersEuclid
                            clusters = KMeans(
                                clusters.flatMap { it.points },
                                centers,
                                grid,
                                distanceMode
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Евклидово")
                    }
                }

                item {
                    Button(
                        onClick = {
                            clusterMode = false
                            clusters = emptyList()
                            differentPoints = emptyList()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Выйти")
                    }
                }
            }
        }
    }
}
@Composable
fun RoadsGridOverlay(
    grid: Array<IntArray>,
    imageWidth: Float,
    imageHeight: Float,
    startX: Float,
    startY: Float
) {
    val rows = grid.size
    val cols = grid[0].size

    Canvas(modifier = Modifier.fillMaxSize()) {

        val cellWidth = imageWidth / cols
        val cellHeight = imageHeight / rows

        for (y in 0 until rows) {
            for (x in 0 until cols) {

                val drawX = startX + x * cellWidth
                val drawY = startY + y * cellHeight

                if (grid[y][x] == 1) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(drawX, drawY),
                        size = androidx.compose.ui.geometry.Size(
                            cellWidth,
                            cellHeight
                        )
                    )
                }

                drawRect(
                    color = Color.Gray,
                    topLeft = Offset(drawX, drawY),
                    size = androidx.compose.ui.geometry.Size(
                        cellWidth,
                        cellHeight
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 0.8f
                    )
                )
            }
        }
    }
}

@Composable
fun AlgorithmButtonsGrid() {
    val algorithms = listOf(
        "A*",
        "Кластеры",
        "Генетический",
        "Муравьиный",
        "Дерево",
        "Нейросеть"
    )
    Surface(
        color = TsuWhite,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 8.dp
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.padding(8.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(algorithms.size){index ->
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = TsuBlue, contentColor = TsuWhite),
                    modifier = Modifier.padding(4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = algorithms[index],
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

fun findNearestRoad(grid: Array<IntArray>, startX: Int, startY: Int): Pair<Int, Int>? {

    val rows = grid.size
    val cols = grid[0].size

    val visited = Array(rows) { BooleanArray(cols) }
    val queue = ArrayDeque<Pair<Int, Int>>()

    queue.add(startX to startY)
    visited[startY][startX] = true

    val directions = listOf(
        1 to 0, -1 to 0,
        0 to 1, 0 to -1
    )

    while (queue.isNotEmpty()) {
        val (x, y) = queue.removeFirst()

        if (grid[y][x] == 1) return x to y

        for ((dx, dy) in directions) {
            val nx = x + dx
            val ny = y + dy

            if (nx in 0 until cols && ny in 0 until rows && !visited[ny][nx]) {
                visited[ny][nx] = true
                queue.add(nx to ny)
            }
        }
    }

    return null
}

data class Node(
    val x: Int,
    val y: Int,
    var g: Int = Int.MAX_VALUE,
    var h: Int = 0,
    var parent: Node? = null
) {
    val f get() = g + h
}

fun aStar(
    grid: Array<IntArray>,
    start: Pair<Int, Int>,
    end: Pair<Int, Int>
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
            if (grid[ny][nx] != 1) continue

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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TSUMapTheme {
        Greeting("Android")
    }
}

