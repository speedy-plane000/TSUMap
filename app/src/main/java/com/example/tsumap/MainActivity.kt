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
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.layout.Row
import android.content.Context
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.tsumap.ui.theme.TsuBlue
import com.example.tsumap.ui.theme.TsuWhite
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableFloatStateOf

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
    LaunchedEffect(Unit) {
        requestLocationPermission(context)
    }
    var steps by remember { mutableStateOf<List<AStarStep>>(emptyList()) }
    var currentStep by remember { mutableStateOf(0) }
    var path by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    LaunchedEffect(steps) {
        if (steps.isNotEmpty()) {
            for (i in steps.indices) {
                currentStep = i
                kotlinx.coroutines.delay(50)
            }

            path = steps.last().path
        }
    }
    val grid = remember {
        loadGrid(context)
    }

    var landmarks by remember {
        mutableStateOf(listOf(
            Landmark("Геолокация сейчас", Point(0, 0), isUserLocation = true),
            Landmark("Декоративный Домик", Point(28, 75)),
            Landmark("Ботанический сад", Point(55, 66)),
            Landmark("Скульптура Белки", Point(116, 88)),
            Landmark("Шахматы", Point(79, 99)),
            Landmark("Камень", Point(110, 101)),
            Landmark("Озеро", Point(91, 31)),
            Landmark("Деревянная Арка", Point(63, 72)),
            Landmark("Мечеть Белая", Point(78, 6)),
            Landmark("Арт-объект Птицы", Point(131, 95)),
            Landmark("Стрит-арт Хамелеон", Point(141, 47))
        ))
    }

    var obstacles by remember { mutableStateOf(mutableSetOf<Pair<Int, Int>>()) }
    var redrawTrigger by remember { mutableStateOf(0) }
    var obstacleMode by remember { mutableStateOf(false) }
    var aStarMode by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var isAcoMode by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showRoads by remember { mutableStateOf(false) }
    var startPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var endPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectionMode by remember { mutableStateOf<String?>(null) }
    var clusters by remember { mutableStateOf<List<Cluster>>(emptyList()) }
    var differentPoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var clusterMode by remember { mutableStateOf(false) }
    var centers by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var distanceMode by remember { mutableStateOf(DistanceMode.EUCLIDEAN) }
    val clusterPoints = remember {
        listOf(
            Point(91, 136), Point(5, 123), Point(14, 1), Point(70, 0),
            Point(149, 142), Point(161, 34), Point(150, 8), Point(111, 68),
            Point(116, 70), Point(114, 71), Point(99, 52), Point(142, 104),
            Point(151, 127)
        )
    }
    val initialCentersEuclid = listOf(
        Point(91, 140),
        Point(70, 8),
        Point(111, 67)
    ).map { it.x.toFloat() to it.y.toFloat() }

    val initialCentersAStar = listOf(
        Point(91, 140),
        Point(70, 8),
        Point(111, 67)
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

                            if (!obstacleMode && selectionMode == null) return@detectTapGestures

                            val tapOnImageX = tapOffset.x - startX
                            val tapOnImageY = tapOffset.y - startY

                            if (tapOnImageX < 0 || tapOnImageY < 0 ||
                                tapOnImageX > actualVisualWidth || tapOnImageY > actualVisualHeight
                            ) return@detectTapGestures

                            val rows = grid.size
                            val cols = grid[0].size

                            val cellX = (tapOnImageX / actualVisualWidth * cols).toInt()
                                .coerceIn(0, cols - 1)
                            val cellY = (tapOnImageY / actualVisualHeight * rows).toInt()
                                .coerceIn(0, rows - 1)

                            var finalX = cellX
                            var finalY = cellY

                            if (grid[cellY][cellX] != 1) {
                                val nearest = findNearestRoad(grid, cellX, cellY)
                                if (nearest != null) {
                                    finalX = nearest.first
                                    finalY = nearest.second
                                } else return@detectTapGestures
                            }

                            // ✅ 1. РЕЖИМ ПРЕПЯТСТВИЙ — ПОЛНОСТЬЮ ОТДЕЛЬНО
                            if (obstacleMode) {
                                val cell = finalX to finalY

                                if (cell in obstacles) {
                                    obstacles.remove(cell)
                                } else {
                                    obstacles.add(cell)
                                }
                                redrawTrigger++

                                // ❗ сбрасываем анимацию ВСЕГДА
                                steps = emptyList()

                                // ❗ пересчитываем путь ТОЛЬКО если есть 2 точки
                                if (startPoint != null && endPoint != null) {
                                    path = aStar(grid, startPoint!!, endPoint!!, obstacles)
                                }

                                return@detectTapGestures // ⛔ ВАЖНО: не идём дальше
                            }

                            // ✅ 2. РЕЖИМ ВЫБОРА ТОЧЕК
                            if (selectionMode == "start") {
                                startPoint = finalX to finalY
                            } else if (selectionMode == "end") {
                                endPoint = finalX to finalY
                            }

                            steps = emptyList()

                            if (startPoint != null && endPoint != null) {
                                path = aStar(grid, startPoint!!, endPoint!!, obstacles)
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

                if (
                    obstacleMode ||
                    obstacles.isNotEmpty() ||
                    (aStarMode && path.isNotEmpty()) ||
                    (clusterMode && clusters.isNotEmpty()) ||
                    (isAcoMode && landmarks.any { it.selected })
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        redrawTrigger

                        val cellWidth = actualVisualWidth / grid[0].size
                        val cellHeight = actualVisualHeight / grid.size

                        obstacles.forEach { (x, y) ->
                            drawRect(
                                color = Color.Red.copy(alpha = 0.4f),
                                topLeft = Offset(
                                    startX + x * cellWidth,
                                    startY + y * cellHeight
                                ),
                                size = androidx.compose.ui.geometry.Size(
                                    cellWidth,
                                    cellHeight
                                )
                            )
                        }

                        if (path.isNotEmpty()  && steps.isEmpty()) {
                            if (path.size > 1) {
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
                        if (isAcoMode) {
                            val selected = landmarks.filter { it.selected }

                            selected.forEachIndexed { index, landmark ->

                                val px =
                                    startX + (landmark.point.x + 0.5f) / grid[0].size * actualVisualWidth
                                val py =
                                    startY + (landmark.point.y + 0.5f) / grid.size * actualVisualHeight

                                if (index == 0) {
                                    drawCircle(
                                        color = TsuBlue,
                                        radius = 16f,
                                        center = Offset(px, py)
                                    )
                                    drawCircle(
                                        color = TsuWhite,
                                        radius = 10f,
                                        center = Offset(px, py)
                                    )
                                } else {
                                    drawCircle(
                                        color = TsuWhite,
                                        radius = 16f,
                                        center = Offset(px, py)
                                    )
                                    drawCircle(
                                        color = TsuBlue,
                                        radius = 10f,
                                        center = Offset(px, py)
                                    )
                                }
                            }
                        }
                        if (aStarMode && steps.isNotEmpty()) {

                            val cellWidth = actualVisualWidth / grid[0].size
                            val cellHeight = actualVisualHeight / grid.size

                            val step = steps[currentStep]

                            step.closedSet.forEach { (x, y) ->
                                drawRect(
                                    color = TsuBlue,
                                    topLeft = Offset(
                                        startX + x * cellWidth,
                                        startY + y * cellHeight
                                    ),
                                    size = androidx.compose.ui.geometry.Size(
                                        cellWidth,
                                        cellHeight
                                    )
                                )
                            }

                            step.openSet.forEach { (x, y) ->
                                drawRect(
                                    color = TsuBlue.copy(alpha = 0.2f),
                                    topLeft = Offset(
                                        startX + x * cellWidth,
                                        startY + y * cellHeight
                                    ),
                                    size = androidx.compose.ui.geometry.Size(
                                        cellWidth,
                                        cellHeight
                                    )
                                )
                            }
                        }
                    }
                }

                if (aStarMode) {
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
        }

        LazyRow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (!clusterMode  && !aStarMode) {

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
                            aStarMode = true
                            clusterMode = false
                            isAcoMode = false

                            path = emptyList()
                            startPoint = null
                            endPoint = null
                            selectionMode = null

                            landmarks = landmarks.map { it.copy(selected = false) }
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
                            clusterMode = true
                            isAcoMode = false

                            path = emptyList()
                            startPoint = null
                            endPoint = null
                            selectionMode = null

                            landmarks = landmarks.map { it.copy(selected = false) }
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
                        onClick = { showSheet = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Муравьиный алгоритм")
                    }
                }
            } else if (aStarMode) {
                item {
                    Button(
                        onClick = {
                            obstacleMode = false
                            selectionMode = "start"
                            steps = emptyList()
                        },
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
                        onClick = {
                            obstacleMode = false
                            selectionMode = "end"
                            steps = emptyList()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Финиш")
                    }
                }

                item {
                    Button(
                        onClick = {
                            getCurrentLocation(context) { lat, lng ->
                                val point = mapLatLngToGrid(lat, lng, grid)
                                val snapped = findNearestRoad(grid, point.x, point.y)
                                startPoint = snapped ?: (point.x to point.y)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Моё местоположение")
                    }
                }

                item {
                    Button(
                        onClick = {
                            obstacleMode = !obstacleMode

                            if (obstacleMode) {
                                selectionMode = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Препятствия")
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (startPoint != null && endPoint != null) {
                                steps = aStarWithSteps(
                                    grid,
                                    startPoint!!,
                                    endPoint!!,
                                    obstacles
                                )
                                currentStep = 0
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Анимация")
                    }
                }

                item {
                    Button(
                        onClick = {
                            aStarMode = false
                            selectionMode = null
                            path = emptyList()
                            obstacles.clear()
                            steps = emptyList()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Назад")
                    }
                }
            } else {
                item {
                    Button(
                        onClick = {
                            val roadPoints = snapPointsToRoad(grid, clusterPoints)
                            centers = snapCentersToRoad(grid, initialCentersAStar)
                            println("$centers")
                            println("$roadPoints")
                            clusters = kMeans(roadPoints, centers, grid, DistanceMode.ASTAR)
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
                            val roadPoints = snapPointsToRoad(grid, clusterPoints)
                            clusters = kMeans(roadPoints, centers, grid, distanceMode)
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
                        Text("Назад")
                    }
                }
            }
        }
        if (showSheet) {
            LandmarkSelectionSheet(
                landmarks = landmarks,
                onToggle = { index ->
                    val lm = landmarks[index]

                    if (lm.isUserLocation) {
                        getCurrentLocation(context) { lat, lng ->

                            val mappedPoint = mapLatLngToGrid(lat, lng, grid)

                            println("GPS: $lat $lng -> GRID: ${mappedPoint.x}, ${mappedPoint.y}")

                            val snapped = findNearestRoad(grid, mappedPoint.x, mappedPoint.y)

                            val finalPoint = if (snapped != null) {
                                Point(snapped.first, snapped.second)
                            } else mappedPoint

                            landmarks = landmarks.toMutableList().also {
                                it[index] = it[index].copy(
                                    selected = true,
                                    point = finalPoint
                                )
                            }
                        }
                    } else {
                        landmarks = landmarks.toMutableList().also {
                            it[index] = it[index].copy(
                                selected = !it[index].selected
                            )
                        }
                    }
                },
                onStart = {
                    val selected = landmarks.filter { it.selected }

                    if (selected.size >= 2) {

                        isAcoMode = true

                        startPoint = null
                        endPoint = null
                        path = emptyList()

                        path = antColonyPath(
                            grid,
                            selected.map { it.point }
                        )
                    }

                    showSheet = false
                },
                onClose = { showSheet = false }
            )
        }
    }
}

fun requestLocationPermission(context: Context) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            context as ComponentActivity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1001
        )
    }
}

@SuppressLint("MissingPermission")
fun getCurrentLocation(
    context: Context,
    onLocation: (Double, Double) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onLocation(location.latitude, location.longitude)
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

@Composable
fun LandmarkSelectionSheet(
    landmarks: List<Landmark>,
    onToggle: (Int) -> Unit,
    onStart: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {

            Text("Выберите достопримечательности")

            landmarks.forEachIndexed { index, lm ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Checkbox(
                        checked = lm.selected,
                        onCheckedChange = { onToggle(index) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = TsuBlue,
                            uncheckedColor = TsuBlue
                        )
                    )

                    Text(
                        lm.name,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TsuBlue,
                    contentColor = TsuWhite
                )
            ) {
                Text("Построить маршрут")
            }

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TsuBlue,
                    contentColor = TsuWhite
                )
            ) {
                Text("Закрыть")
            }
        }
    }
}

