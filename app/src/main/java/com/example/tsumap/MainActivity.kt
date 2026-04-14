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
import android.graphics.BitmapFactory
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
import com.google.android.gms.location.LocationServices
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

@SuppressLint("UnusedBoxWithConstraintsScope", "MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
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
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.tsu_pixel)

    data class CafePoint(
        val name: String,
        val point: Point
    )

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
    var geneticMode by remember { mutableStateOf( false) }
    var showGeneticItemsSheet by remember { mutableStateOf(false) }
    var geneticStartPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var geneticStops by remember {mutableStateOf<List<Point>>(emptyList())}
    var selectedNeeds by remember { mutableStateOf(setOf<String>()) }
    var showSheet by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
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
    val cafePoints = remember {
        listOf(
            CafePoint("Мария-Ра", Point(15, 4)),
            CafePoint("Цзисяни", Point(101, 6)),
            CafePoint("Безумно", Point(149, 9)),
            CafePoint("Абрикос", Point(149, 15)),
            CafePoint("Пилад", Point(160, 33)),
            CafePoint("XO Bakery", Point(92, 54)),
            CafePoint("Сыр-Бор", Point(135, 60)),
            CafePoint("Сибирские блины (ЦК)", Point(104, 64)),
            CafePoint("Укромное местечко", Point(167, 89)),
            CafePoint("Научка", Point(76, 97)),
            CafePoint("Сибирские блины (Ленина)", Point(172, 116)),
            CafePoint("Rostiks", Point(102, 120)),
            CafePoint("Гербарий", Point(122, 122)),
            CafePoint("Пятерочка", Point(4, 124)),
            CafePoint("Ближе", Point(150, 131)),
            CafePoint("Бристоль", Point(92, 137)),
            CafePoint("Ярче", Point(148, 144))
        )
    }
    val initialCentersEuclid = listOf(
        Point(92, 137),
        Point(104, 64),
        Point(101, 6)
    ).map { it.x.toFloat() to it.y.toFloat() }

    val initialCentersAStar = listOf(
        Point(92, 137),
        Point(104, 64),
        Point(101, 6)
    ).map { it.x.toFloat() to it.y.toFloat() }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
            .background(TsuWhite)
    ) {
        TopAppBar(
            title = {Text(text = "Map", color = TsuWhite)},
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = TsuBlue,
                titleContentColor = TsuWhite,
                actionIconContentColor = TsuWhite
            ),
            navigationIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.tsu_logo),
                    contentDescription = "Логотип",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            },
            actions = {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Меню"
                    )
                }
                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("A*") },
                        onClick = {
                            isMenuExpanded = false
                            aStarMode = true
                            clusterMode = false
                            isAcoMode = false
                            path = emptyList()
                            geneticStops = emptyList()
                            startPoint = null
                            endPoint = null
                            selectionMode = null
                            steps = emptyList()
                            landmarks = landmarks.map { it.copy(selected = false) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Кластеры") },
                        onClick = {
                            isMenuExpanded = false
                            clusterMode = true
                            aStarMode = false
                            isAcoMode = false
                            path = emptyList()
                            geneticStops = emptyList()
                            startPoint = null
                            endPoint = null
                            selectionMode = null
                            steps = emptyList()
                            landmarks = landmarks.map { it.copy(selected = false) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Муравьиный") },
                        onClick = {
                            isMenuExpanded = false
                            aStarMode = false
                            clusterMode = false
                            isAcoMode = false
                            path = emptyList()
                            geneticStops = emptyList()
                            startPoint = null
                            endPoint = null
                            selectionMode = null
                            steps = emptyList()
                            landmarks = landmarks.map { it.copy(selected = false) }
                            showSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Генетический") },
                        onClick = {
                            isMenuExpanded = false

                            aStarMode = false
                            clusterMode = false
                            isAcoMode = false
                            geneticMode = false

                            path = emptyList()
                            geneticStops = emptyList()
                            steps = emptyList()
                            startPoint = null
                            endPoint = null


                            selectionMode = "genetic_start"
                            geneticStartPoint = null
                            selectedNeeds = emptySet()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Дерево Решений") },
                        onClick = {
                            isMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Оценка заведения") },
                        onClick = {
                            isMenuExpanded = false
                        }
                    )
                }
            }
        )

        Box(
            modifier = Modifier.weight(1f)
                .fillMaxWidth()
        ){
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val density = LocalDensity.current

                val boxWidth = constraints.maxWidth.toFloat()
                val boxHeight = constraints.maxHeight.toFloat()
                val isMapRotated = true

                val imageWidth = 686f
                val imageHeight = 563f

                val imgRatio = imageWidth / imageHeight
                val boxRatio = boxWidth / boxHeight

                val actualVisualWidth = if (imgRatio > boxRatio) boxWidth else boxHeight * imgRatio
                val actualVisualHeight = if (imgRatio > boxRatio) boxWidth / imgRatio else boxHeight

                val baseScaleX = boxWidth / actualVisualWidth
                val baseScaleY = boxHeight / actualVisualHeight
                val baseScale = maxOf(baseScaleX, baseScaleY)

                LaunchedEffect(baseScale) {
                    if (scale < baseScale) {
                        scale = baseScale
                        offset = Offset.Zero
                    }
                }

                val minScale = baseScale
                val maxScale = baseScale * 4f

                val startX = (boxWidth - actualVisualWidth) / 2f
                val startY = (boxHeight - actualVisualHeight) / 2f


                fun gridToScreenPx(cellX: Float, cellY: Float, cols: Int, rows: Int): Offset {
                    val xNorm = cellX / cols.toFloat()
                    val yNorm = cellY / rows.toFloat()

                    val x0 = startX + xNorm * actualVisualWidth
                    val y0 = startY + yNorm * actualVisualHeight
                    if (!isMapRotated) return Offset(x0, y0)

                    val cx = startX + actualVisualWidth / 2f
                    val cy = startY + actualVisualHeight / 2f
                    val dx = x0 - cx
                    val dy = y0 - cy
                    return Offset(
                        x = cx - dy,
                        y = cy + dx
                    )
                }
                fun screenToGridCell(tapX: Float, tapY: Float, cols: Int, rows: Int): Pair<Int, Int>? {
                    val xUnrot: Float
                    val yUnrot: Float
                    if (!isMapRotated) {
                        xUnrot = tapX
                        yUnrot = tapY
                    } else {

                        val cx = startX + actualVisualWidth / 2f
                        val cy = startY + actualVisualHeight / 2f
                        val dx = tapX - cx
                        val dy = tapY - cy
                        xUnrot = cx + dy
                        yUnrot = cy - dx
                    }
                    val localX = xUnrot - startX
                    val localY = yUnrot - startY
                    if (localX < 0f || localY < 0f || localX > actualVisualWidth || localY > actualVisualHeight) {
                        return null
                    }
                    val xNorm = localX / actualVisualWidth
                    val yNorm = localY / actualVisualHeight
                    val cellX = (xNorm * cols).toInt().coerceIn(0, cols - 1)
                    val cellY = (yNorm * rows).toInt().coerceIn(0, rows - 1)
                    return cellX to cellY
                }

                val transformableState = rememberTransformableState { zoomChange, panChange, _ ->

                    val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)

                    val panContentWidth = if (isMapRotated) actualVisualHeight else actualVisualWidth
                    val panContentHeight = if (isMapRotated) actualVisualWidth else actualVisualHeight

                    val maxX = maxOf(0f, (panContentWidth * newScale - boxWidth) / 2f)
                    val maxY = maxOf(0f, (panContentHeight * newScale - boxHeight) / 2f)


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
                            translationY = offset.y,
                        )
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { tapOffset ->

                                    if (!obstacleMode && selectionMode == null) return@detectTapGestures

                                    val rows = grid.size
                                    val cols = grid[0].size
                                    val mapped = screenToGridCell(tapOffset.x, tapOffset.y, cols, rows)
                                        ?: return@detectTapGestures
                                    val cellX = mapped.first
                                    val cellY = mapped.second

                                    var finalX = cellX
                                    var finalY = cellY

                                    if (grid[cellY][cellX] != 1) {
                                        val nearest = findNearestRoad(grid, cellX, cellY)
                                        if (nearest != null) {
                                            finalX = nearest.first
                                            finalY = nearest.second
                                        } else return@detectTapGestures
                                    }

                                    if (obstacleMode) {
                                        val cell = finalX to finalY

                                        if (cell in obstacles) {
                                            obstacles.remove(cell)
                                        } else {
                                            obstacles.add(cell)
                                        }
                                        redrawTrigger++

                                        steps = emptyList()

                                        if (startPoint != null && endPoint != null) {
                                            path = aStar(grid, startPoint!!, endPoint!!, obstacles)
                                        }

                                        return@detectTapGestures
                                    }

                                    if (selectionMode == "start") {
                                        startPoint = finalX to finalY
                                        android.util.Log.d(
                                            "TAP_COORDS",
                                            "Start: x=$finalX, y=$finalY"
                                        )
                                    } else if (selectionMode == "end") {
                                        endPoint = finalX to finalY
                                    }else if(selectionMode == "genetic_start"){
                                        geneticStartPoint = finalX to finalY
                                        selectionMode = null
                                        showGeneticItemsSheet = true
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
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = if (isMapRotated) 90f else 0f
                                    transformOrigin = TransformOrigin.Center
                                }
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
                            (isAcoMode && landmarks.any { it.selected }) ||
                            (geneticMode && path.isNotEmpty())
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                redrawTrigger

                                val cellWidth = actualVisualWidth / grid[0].size
                                val cellHeight = actualVisualHeight / grid.size

                                obstacles.forEach { (x, y) ->
                                    val p = gridToScreenPx(x + 0.5f, y + 0.5f, grid[0].size, grid.size)
                                    drawCircle(
                                        color = Color.Red.copy(alpha = 0.6f),
                                        radius = 6f,
                                        center = p
                                    )
                                }


                                if (path.isNotEmpty()  && steps.isEmpty()) {
                                    if (path.size > 1) {
                                        for (i in 0 until path.size - 1) {
                                            val (x1, y1) = path[i]
                                            val (x2, y2) = path[i + 1]

                                            val p1 = gridToScreenPx(x1 + 0.5f, y1 + 0.5f, grid[0].size, grid.size)
                                            val p2 = gridToScreenPx(x2 + 0.5f, y2 + 0.5f, grid[0].size, grid.size)
                                            drawLine(
                                                color = TsuBlue,
                                                start = p1,
                                                end = p2,
                                                strokeWidth = 6f
                                            )
                                        }
                                    }
                                }
                                if (clusters.isNotEmpty()) {
                                    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Magenta)

                                    clusters.forEachIndexed { index, cluster ->

                                        val fillColor = when (index) {
                                            0 -> TsuBlue
                                            1 -> Color.Red
                                            2 -> Color.Green
                                            else -> Color.Magenta
                                        }
                                        val hull = convexHull(cluster.points)
                                        if (hull.size >= 3) {
                                            val path = androidx.compose.ui.graphics.Path().apply {
                                                val first = gridToScreenPx(
                                                    hull[0].x + 0.5f,
                                                    hull[0].y + 0.5f,
                                                    grid[0].size,
                                                    grid.size
                                                )
                                                moveTo(first.x, first.y)
                                                for (i in 1 until hull.size) {
                                                    val p = gridToScreenPx(
                                                        hull[i].x + 0.5f,
                                                        hull[i].y + 0.5f,
                                                        grid[0].size,
                                                        grid.size
                                                    )
                                                    lineTo(p.x, p.y)
                                                }
                                                close()
                                            }

                                            drawPath(
                                                path = path,
                                                color = fillColor.copy(alpha = 0.20f)
                                            )

                                            drawPath(
                                                path = path,
                                                color = fillColor.copy(alpha = 0.90f),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                                            )
                                        }

                                        cluster.points.forEach { point ->
                                            val p = gridToScreenPx(point.x + 0.5f, point.y + 0.5f, grid[0].size, grid.size)
                                            val px = p.x
                                            val py = p.y

                                            when (index) {

                                                0 -> {
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
                                                }


                                                1 -> {
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

                                                2 -> {
                                                    drawCircle(
                                                        color = TsuBlue,
                                                        radius = 14f,
                                                        center = Offset(px, py)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    differentPoints.forEach { point ->

                                        val p = gridToScreenPx(point.x + 0.5f, point.y + 0.5f, grid[0].size, grid.size)
                                        drawCircle(
                                            color = Color.Yellow,
                                            radius = 12f,
                                            center = p
                                        )
                                    }
                                }
                                if (isAcoMode) {
                                    val selected = landmarks.filter { it.selected }

                                    selected.forEachIndexed { index, landmark ->

                                        val p = gridToScreenPx(
                                            landmark.point.x + 0.5f,
                                            landmark.point.y + 0.5f,
                                            grid[0].size,
                                            grid.size
                                        )
                                        val px = p.x
                                        val py = p.y

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
                                        val p = gridToScreenPx(x + 0.5f, y + 0.5f, grid[0].size, grid.size)
                                        drawCircle(color = TsuBlue, radius = 4f, center = p)
                                    }


                                    step.openSet.forEach { (x, y) ->
                                        val p = gridToScreenPx(x + 0.5f, y + 0.5f, grid[0].size, grid.size)
                                        drawCircle(color = TsuBlue.copy(alpha = 0.25f), radius = 4f, center = p)
                                    }
                                }

                            }
                        }

                        if (aStarMode) {
                            startPoint?.let { (x, y) ->

                                val p = gridToScreenPx(x + 0.5f, y + 0.5f, grid[0].size, grid.size)
                                val px = p.x
                                val py = p.y

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

                                val p = gridToScreenPx(x + 0.5f, y + 0.5f, grid[0].size, grid.size)
                                val px = p.x
                                val py = p.y

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
                        if (geneticMode && geneticStops.isNotEmpty()) {
                            if (geneticMode) {
                                geneticStartPoint?.let { (x, y) ->
                                    val p = gridToScreenPx(x + 0.5f, y + 0.5f, grid[0].size, grid.size)
                                    val startSize = 20.dp
                                    val startRadiusPx = with(density) { (startSize / 2).toPx() }
                                    Box(
                                        modifier = Modifier
                                            .offset {
                                                IntOffset(
                                                    (p.x - startRadiusPx).toInt(),
                                                    (p.y - startRadiusPx).toInt()
                                                )
                                            }
                                            .size(startSize)
                                            .background(TsuWhite, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(TsuBlue, CircleShape)
                                        )
                                    }
                                }
                            }
                            geneticStops.forEachIndexed { index, point ->
                                val p = gridToScreenPx(point.x + 0.5f, point.y + 0.5f, grid[0].size, grid.size)
                                val outerSize = 24.dp
                                val innerSize = 18.dp
                                val outerRadiusPx = with(density) { (outerSize / 2).toPx() }
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                (p.x - outerRadiusPx).toInt(),
                                                (p.y - outerRadiusPx).toInt()
                                            )
                                        }
                                        .size(outerSize)
                                        .background(TsuWhite, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(innerSize)
                                            .background(TsuBlue, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (index + 1).toString(),
                                            color = TsuWhite,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = { showRoads = !showRoads },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(TsuWhite.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = if (showRoads) "Скрыть карту" else "Показать карту",
                        tint = TsuBlue
                    )
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
                                onClick = {
                                    aStarMode = true
                                    clusterMode = false
                                    isAcoMode = false

                                    path = emptyList()
                                    geneticStops = emptyList()
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
                                    geneticStops = emptyList()
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
                                    geneticStops = emptyList()
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
                                    val roadPoints = snapPointsToRoad(grid, cafePoints.map { it.point })
                                    centers = snapCentersToRoad(grid, initialCentersAStar)
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
                                    val roadPoints = snapPointsToRoad(grid, cafePoints.map { it.point })
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
                                geneticStops = emptyList()

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
                if (showGeneticItemsSheet) {
                    GeneticItemsSheet(
                        selected = selectedNeeds,
                        onToggle = { key ->
                            selectedNeeds = if (key in selectedNeeds) selectedNeeds - key else selectedNeeds + key
                        },
                        onStart = onStart@{
                            val start = geneticStartPoint ?: run {
                                showGeneticItemsSheet = false
                                return@onStart
                            }

                            val needTags = itemTagsToNeed(selectedNeeds)
                            if (needTags.isEmpty()) {
                                showGeneticItemsSheet = false
                                return@onStart
                            }

                            showGeneticItemsSheet = false
                            geneticMode = true
                            path = emptyList()
                            geneticStops = emptyList()

                            scope.launch(Dispatchers.Default) {
                                val startPoint = Point(start.first, start.second)
                                val snappedStartPair = findNearestRoad(grid, startPoint.x, startPoint.y)
                                val snappedStart = if (snappedStartPair != null) {
                                    Point(snappedStartPair.first, snappedStartPair.second)
                                } else {
                                    startPoint
                                }

                                val rawCandidates = candidatesFor(needTags, foodVenuesCatalog())
                                val candidates = rawCandidates.map { place ->
                                    val s = findNearestRoad(grid, place.point.x, place.point.y)
                                    if (s != null) place.copy(point = Point(s.first, s.second)) else place
                                }

                                if (candidates.isEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        path = emptyList()
                                        geneticStops = emptyList()
                                    }
                                    return@launch
                                }

                                val config = RouteConfig(kmPerGridUnit = 0.01)

                                val (finalEval, _) = runGeneticAlgorithm(
                                    startPoint = snappedStart,
                                    startTime = java.time.LocalDateTime.now(),
                                    need = needTags,
                                    venues = candidates,
                                    config = config,
                                    onGeneration = { _, bestEval, _ ->
                                        val stops = bestEval.visitedOrder.map { idx -> candidates[idx].point }
                                        val previewPath = stitchAstarPath(grid, snappedStart, stops)

                                        scope.launch(Dispatchers.Main) {
                                            path = previewPath
                                            geneticStops = stops
                                        }

                                        Thread.sleep(100)
                                    }
                                )

                                val finalStops = finalEval.visitedOrder.map { idx -> candidates[idx].point }
                                val finalPath = stitchAstarPath(grid, snappedStart, finalStops)

                                withContext(Dispatchers.Main) {
                                    path = finalPath
                                    geneticStops = finalStops
                                }
                            }
                        },
                        onClose = { showGeneticItemsSheet = false }
                    )
                }
            }
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

@Composable
fun GeneticItemsSheet(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onStart: () -> Unit,
    onClose: () -> Unit
) {
    val items = listOf(
        "Одноразовая посуда",
        "Рамен/Вок/Рис",
        "Шаурма",
        "Выпечка",
        "Напитки",
        "Снеки",
        "Кофе",
        "Комплексный обед",
        "Блины",
        "Фастфуд"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            Text("Выберите, что купить")
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item in selected,
                        onCheckedChange = { onToggle(item) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = TsuBlue,
                            uncheckedColor = TsuBlue
                        )
                    )
                    Text(item, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = TsuBlue, contentColor = TsuWhite)
            ) { Text("Запустить генетический") }

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = TsuBlue, contentColor = TsuWhite)
            ) { Text("Закрыть") }
        }
    }
}




