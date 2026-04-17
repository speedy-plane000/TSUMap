package com.example.tsumap

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
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


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MainMapScreen() {
    var decisionTreeMode by remember { mutableStateOf(false) }
    var decisionTreeRoot by remember { mutableStateOf<DecisionNode?>(null) }
    var steps by remember { mutableStateOf<List<AStarStep>>(emptyList()) }
    var currentStep by remember { mutableStateOf(0) }
    var path by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var noPathMessageVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(steps) {
        if (steps.isNotEmpty()) {
            for (i in steps.indices) {
                currentStep = i
                kotlinx.coroutines.delay(10)
            }
            path = steps.last().path
            noPathMessageVisible = steps.last().path.isEmpty()
        }
    }

    LaunchedEffect(Unit) {
        val rows = loadTrainingRows(context, "data.csv")
        if (rows.isNotEmpty()) {
            val features = rows.first().features.keys.toList()
            val root = trainDecisionTree(rows, features, forceRootFeature = "food_type")
            decisionTreeRoot = root
        }
    }
    val grid = remember {
        loadGrid(context)
    }

    data class CafePoint(
        val name: String,
        val point: Point
    )

    var landmarks by remember {
        mutableStateOf(
            listOf(
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
            )
        )
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
    var geneticMode by remember { mutableStateOf(false) }
    var showGeneticItemsSheet by remember { mutableStateOf(false) }
    var geneticStartPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var geneticStops by remember { mutableStateOf<List<Point>>(emptyList()) }
    var selectedNeeds by remember { mutableStateOf(setOf<String>()) }
    var geneticUiMode by remember { mutableStateOf(false) }
    var geneticHintText by remember { mutableStateOf<String?>(null) }
    var startPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var endPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectionMode by remember { mutableStateOf<String?>(null) }
    var clusters by remember { mutableStateOf<List<Cluster>>(emptyList()) }
    var differentPoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var clusterMode by remember { mutableStateOf(false) }
    var centers by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var distanceMode by remember { mutableStateOf(DistanceMode.EUCLIDEAN) }
    var showCafeSelectionDialog by remember { mutableStateOf(false) }
    var selectedCafeForRating by remember { mutableStateOf<String?>(null) }
    var showRatingDialog by remember { mutableStateOf(false) }
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
    val cafeNamesForRating = remember { cafePoints.map { it.name } }
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


    val minScale = 1f
    val maxScale = 8f

    Column(Modifier.fillMaxSize()) {

        TopAppBar(
            title = {Text("Map", color = TsuWhite)},
            navigationIcon = {
                Icon(
                    painter = painterResource(R.drawable.tsu_logo),
                    contentDescription = null,
                    tint = TsuWhite,
                    modifier = Modifier.size(30.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TsuBlue)
        )
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

            fun clampOffset(rawOffset: Offset, s: Float): Offset {
                val cx = boxWidth / 2f
                val cy = boxHeight / 2f

                val maxX = s * (cx - startX) - cx
                val minX = cx + s * (cx - startX - actualVisualWidth)
                val clampedX = if (minX <= maxX) rawOffset.x.coerceIn(minX, maxX) else 0f

                val maxY = s * (cy - startY) - cy
                val minY = cy + s * (cy - startY - actualVisualHeight)
                val clampedY = if (minY <= maxY) rawOffset.y.coerceIn(minY, maxY) else 0f

                return Offset(clampedX, clampedY)
            }

            if (geneticHintText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 64.dp)
                        .background(TsuWhite)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(geneticHintText!!, color = TsuBlue)
                }
            }

            if (aStarMode && noPathMessageVisible && startPoint != null && endPoint != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .statusBarsPadding()
                        .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                        .background(TsuWhite.copy(alpha = 0.9f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Пути нет",
                        color = TsuBlue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape)
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, panChange, zoomChange, _ ->
                            val oldScale = scale
                            val newScale = (oldScale * zoomChange).coerceIn(minScale, maxScale)
                            val scaleFactor = newScale / oldScale

                            val center = Offset(boxWidth / 2f, boxHeight / 2f)
                            val newOffset =
                                (centroid - center) * (1f - scaleFactor) + offset * scaleFactor + panChange

                            scale = newScale
                            offset = clampOffset(newOffset, newScale)
                        }
                    }
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
                                        val newPath = aStar(grid, startPoint!!, endPoint!!, obstacles)
                                        path = newPath
                                        noPathMessageVisible = newPath.isEmpty()
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
                                } else if (selectionMode == "genetic_start") {
                                    geneticStartPoint = finalX to finalY
                                    selectionMode = null
                                    geneticHintText = null
                                    showGeneticItemsSheet = true
                                }

                                steps = emptyList()

                                if (startPoint != null && endPoint != null) {
                                    val newPath = aStar(grid, startPoint!!, endPoint!!, obstacles)
                                    path = newPath
                                    noPathMessageVisible = newPath.isEmpty()
                                } else {
                                    noPathMessageVisible = false
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
                        (aStarMode && steps.isNotEmpty()) ||
                        (clusterMode && clusters.isNotEmpty()) ||
                        (isAcoMode && landmarks.any { it.selected }) ||
                        (geneticMode && path.isNotEmpty())
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            redrawTrigger

                            val cellWidth = actualVisualWidth / grid[0].size
                            val cellHeight = actualVisualHeight / grid.size

                            obstacles.forEach { (x, y) ->
                                drawRect(
                                    color = Color.Red.copy(alpha = 0.6f),
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

                            if (path.isNotEmpty()) {
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


                                val cols = grid[0].size
                                val rows = grid.size

                                clusters.forEachIndexed { index, cluster ->

                                    val fillColor = when (index) {
                                        0 -> TsuBlue
                                        1 -> TsuBlue
                                        2 -> TsuBlue
                                        else -> TsuBlue
                                    }
                                    val hull = convexHull(cluster.points)

                                    if (hull.size >= 3) {
                                        val zonePath = androidx.compose.ui.graphics.Path().apply {

                                            val firstPx = startX + (hull[0].x + 0.5f) / cols * actualVisualWidth
                                            val firstPy = startY + (hull[0].y + 0.5f) / rows * actualVisualHeight

                                            moveTo(firstPx, firstPy)

                                            for (i in 1 until hull.size) {
                                                val px = startX + (hull[i].x + 0.5f) / cols * actualVisualWidth
                                                val py = startY + (hull[i].y + 0.5f) / rows * actualVisualHeight
                                                lineTo(px, py)
                                            }

                                            close()
                                        }

                                        drawPath(
                                            path = zonePath,
                                            color = fillColor.copy(alpha = 0.20f)
                                        )

                                        drawPath(
                                            path = zonePath,
                                            color = fillColor.copy(alpha = 0.90f),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                                        )
                                    }

                                    cluster.points.forEach { point ->
                                        val px =
                                            startX + (point.x + 0.5f) / grid[0].size * actualVisualWidth
                                        val py =
                                            startY + (point.y + 0.5f) / grid.size * actualVisualHeight

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
                                if (step.path.size > 1) {
                                    for (i in 0 until step.path.size - 1) {
                                        val (x1, y1) = step.path[i]
                                        val (x2, y2) = step.path[i + 1]

                                        val px1 = startX + (x1 + 0.5f) * cellWidth
                                        val py1 = startY + (y1 + 0.5f) * cellHeight
                                        val px2 = startX + (x2 + 0.5f) * cellWidth
                                        val py2 = startY + (y2 + 0.5f) * cellHeight

                                        drawLine(
                                            color = TsuWhite,
                                            start = Offset(px1, py1),
                                            end = Offset(px2, py2),
                                            strokeWidth = 6f
                                        )
                                    }
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
                    if (geneticMode) {
                        geneticStartPoint?.let { (x, y) ->
                            val px = startX + (x + 0.5f) / grid[0].size * actualVisualWidth
                            val py = startY + (y + 0.5f) / grid.size * actualVisualHeight
                            val r = with(density) { (10.dp).toPx() }

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((px - r).toInt(), (py - r).toInt()) }
                                    .size(20.dp)
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

                        geneticStops.forEachIndexed { index, p ->
                            val px = startX + (p.x + 0.5f) / grid[0].size * actualVisualWidth
                            val py = startY + (p.y + 0.5f) / grid.size * actualVisualHeight
                            val r = with(density) { (12.dp).toPx() }

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((px - r).toInt(), (py - r).toInt()) }
                                    .size(24.dp)
                                    .background(TsuWhite, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(TsuBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text((index + 1).toString(), color = TsuWhite)
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { showRoads = !showRoads },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TsuBlue,
                    contentColor = TsuWhite
                )
            ) {
                Text(if (showRoads) "Скрыть дороги" else "Показать дороги")
            }

            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (geneticUiMode) {
                    if (geneticStartPoint != null){
                        item {
                            Button(
                                onClick = { showGeneticItemsSheet = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TsuBlue,
                                    contentColor = TsuWhite
                                )
                            ) { Text("Поменять товары") }
                        }

                        item {
                            Button(
                                onClick = {
                                    geneticUiMode = false
                                    geneticMode = false
                                    selectionMode = null
                                    geneticHintText = null
                                    showGeneticItemsSheet = false

                                    path = emptyList()
                                    geneticStops = emptyList()
                                    geneticStartPoint = null
                                    selectedNeeds = emptySet()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TsuBlue,
                                    contentColor = TsuWhite
                                )
                            ) { Text("Назад") }
                        }
                    }
                } else if (!clusterMode && !aStarMode) {
                    item {
                        Button(
                            onClick = {
                                aStarMode = true
                                clusterMode = false
                                isAcoMode = false

                                path = emptyList()
                                noPathMessageVisible = false
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
                            onClick = {
                                aStarMode = false
                                clusterMode = false
                                isAcoMode = false
                                geneticMode = false
                                obstacleMode = false

                                path = emptyList()
                                geneticStops = emptyList()
                                startPoint = null
                                endPoint = null
                                steps = emptyList()

                                selectionMode = "genetic_start"
                                geneticStartPoint = null
                                selectedNeeds = emptySet()

                                geneticUiMode = true
                                geneticHintText = "Выберите точку старта для генетического алгоритма"

                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TsuBlue,
                                contentColor = TsuWhite
                            )
                        ) {
                            Text("Генетический")
                        }
                    }
                    if (geneticUiMode) {
                        item {
                            Button(
                                onClick = { showGeneticItemsSheet = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TsuBlue,
                                    contentColor = TsuWhite
                                )
                            ) { Text("Поменять товары") }
                        }

                        item {
                            Button(
                                onClick = {
                                    geneticUiMode = false
                                    geneticMode = false
                                    selectionMode = null
                                    geneticHintText = null
                                    showGeneticItemsSheet = false

                                    path = emptyList()
                                    geneticStops = emptyList()
                                    geneticStartPoint = null
                                    selectedNeeds = emptySet()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TsuBlue,
                                    contentColor = TsuWhite
                                )
                            ) { Text("Назад") }
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
                    item {
                        Button(
                            onClick = {

                                aStarMode = false
                                clusterMode = false
                                isAcoMode = false
                                geneticMode = false
                                geneticUiMode = false
                                showSheet = false
                                showGeneticItemsSheet = false


                                path = emptyList()
                                steps = emptyList()
                                obstacles.clear()
                                startPoint = null
                                endPoint = null
                                selectionMode = null


                                decisionTreeMode = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TsuBlue,
                                contentColor = TsuWhite
                            )
                        ) {
                            Text("Дерево решений")
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                clusterMode = false
                                aStarMode = false
                                isAcoMode = false
                                selectionMode = null
                                startPoint = null
                                endPoint = null
                                path = emptyList()
                                steps = emptyList()
                                showCafeSelectionDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TsuBlue,
                                contentColor = TsuWhite
                            )
                        ) {
                            Text("Оценка")
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
                                    val computedSteps = aStarWithSteps(
                                        grid,
                                        startPoint!!,
                                        endPoint!!,
                                        obstacles
                                    )
                                    steps = computedSteps
                                    currentStep = 0
                                    path = emptyList()
                                    noPathMessageVisible = computedSteps.lastOrNull()?.path?.isEmpty() ?: true
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
                                noPathMessageVisible = false
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
                        landmarks = landmarks.toMutableList().also {
                            it[index] = it[index].copy(
                                selected = !it[index].selected
                            )
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
            if (showGeneticItemsSheet) {
                GeneticItemsSheet(
                    selected = selectedNeeds,
                    onToggle = { key ->
                        selectedNeeds =
                            if (key in selectedNeeds) selectedNeeds - key else selectedNeeds + key
                    },
                    onStart = onStart@{
                        val start = geneticStartPoint ?: run {
                            showGeneticItemsSheet = false
                            return@onStart
                        }

                        if (selectedNeeds.isEmpty()) {
                            showGeneticItemsSheet = false
                            return@onStart
                        }



                        val result = buildGeneticPathOnGrid(
                            grid = grid,
                            start = Point(start.first, start.second),
                            allCatalog = foodVenuesCatalog(),
                            need = itemTagsToNeed(selectedNeeds),
                        )

                        path = result.path
                        geneticStops = result.stops
                        geneticMode = true
                        showGeneticItemsSheet = false
                        geneticUiMode = true
                        geneticHintText = null
                    },
                    onClose = { showGeneticItemsSheet = false }
                )
            }
            if (decisionTreeMode) {
                DecisionTreeChatScreen(
                    root = decisionTreeRoot,
                    onExit = {
                        decisionTreeMode = false
                    }
                )
            }
            if (showRatingDialog && selectedCafeForRating != null) {
                RatingDrawingDialog(
                    placeName = selectedCafeForRating!!,
                    onClose = { showRatingDialog = false }
                )
            }
            if (showCafeSelectionDialog) {
                CafeSelectionDialog(
                    cafeNames = cafeNamesForRating,
                    onSelect = { cafeName ->
                        selectedCafeForRating = cafeName
                        showCafeSelectionDialog = false
                        showRatingDialog = true
                    },
                    onClose = { showCafeSelectionDialog = false }
                )
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
fun CafeSelectionDialog(
    cafeNames: List<String>,
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TsuWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Выберите заведение")
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cafeNames.size) { index ->
                        val cafe = cafeNames[index]
                        Button(
                            onClick = { onSelect(cafe) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TsuBlue,
                                contentColor = TsuWhite
                            )
                        ) {
                            Text(cafe)
                        }
                    }
                }
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
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
}

@Composable
fun RatingDrawingDialog(
    placeName: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val gridSize = 50
    val pixels = remember { Array(gridSize) { BooleanArray(gridSize) } }
    var redrawTrigger by remember { mutableStateOf(0) }
    var predictedResult by remember { mutableStateOf<String?>(null) }
    var lastDragCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val (classifier, modelReady) = remember {
        val model =
            DigitClassifier(inputSize = 50 * 50, hiddenSize = 128, numClasses = 10, seed = 42)
        val loaded = try {
            model.loadFromAssets(context, "digit_model.bin")
            true
        } catch (_: Exception) {
            false
        }
        model to loaded
    }

    fun paintBrush(cellX: Int, cellY: Int) {
        for (dy in -1..1) {
            for (dx in -1..1) {
                val nx = cellX + dx
                val ny = cellY + dy
                if (nx in 0 until gridSize && ny in 0 until gridSize) {
                    pixels[ny][nx] = true
                }
            }
        }
        redrawTrigger++
    }

    fun paintLine(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        val dx = to.first - from.first
        val dy = to.second - from.second
        val steps = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
        if (steps == 0) {
            paintBrush(from.first, from.second)
            return
        }

        for (step in 0..steps) {
            val t = step.toFloat() / steps.toFloat()
            val x = (from.first + dx * t).toInt().coerceIn(0, gridSize - 1)
            val y = (from.second + dy * t).toInt().coerceIn(0, gridSize - 1)
            paintBrush(x, y)
        }
    }

    fun clearCanvas() {
        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                pixels[y][x] = false
            }
        }
        predictedResult = null
        redrawTrigger++
    }

    Dialog(onDismissRequest = onClose) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = TsuWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Оценка: $placeName")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val cellSize = size.width / gridSize
                                    val cellX =
                                        (offset.x / cellSize).toInt().coerceIn(0, gridSize - 1)
                                    val cellY =
                                        (offset.y / cellSize).toInt().coerceIn(0, gridSize - 1)
                                    paintBrush(cellX, cellY)
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        val cellSize = size.width / gridSize
                                        val sx = (startOffset.x / cellSize).toInt()
                                            .coerceIn(0, gridSize - 1)
                                        val sy = (startOffset.y / cellSize).toInt()
                                            .coerceIn(0, gridSize - 1)
                                        val startCell = sx to sy
                                        paintBrush(sx, sy)
                                        lastDragCell = startCell
                                    },
                                    onDragEnd = { lastDragCell = null },
                                    onDragCancel = { lastDragCell = null }
                                ) { change, _ ->
                                    val cellSize = size.width / gridSize
                                    val cellX =
                                        (change.position.x / cellSize).toInt()
                                            .coerceIn(0, gridSize - 1)
                                    val cellY =
                                        (change.position.y / cellSize).toInt()
                                            .coerceIn(0, gridSize - 1)
                                    val currentCell = cellX to cellY
                                    val previousCell = lastDragCell
                                    if (previousCell != null) {
                                        paintLine(previousCell, currentCell)
                                    } else {
                                        paintBrush(cellX, cellY)
                                    }
                                    lastDragCell = currentCell
                                }
                            }
                    ) {
                        redrawTrigger
                        val cellWidth = size.width / gridSize
                        val cellHeight = size.height / gridSize

                        for (y in 0 until gridSize) {
                            for (x in 0 until gridSize) {
                                drawRect(
                                    color = if (pixels[y][x]) Color.Black else Color.White,
                                    topLeft = Offset(x * cellWidth, y * cellHeight),
                                    size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight)
                                )
                            }
                        }
                    }
                }

                if (predictedResult != null) {
                    Text(predictedResult!!)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            if (!modelReady) {
                                predictedResult =
                                    "Модель не обучена. Запустите train_mnist.main.kts на ПК."
                            } else {
                                val input = FloatArray(gridSize * gridSize) { idx ->
                                    val y = idx / gridSize
                                    val x = idx % gridSize
                                    if (pixels[y][x]) 1f else 0f
                                }
                                val digit = classifier.predict(input)
                                predictedResult = "Оценка: $digit"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Оценить")
                    }
                    Button(
                        onClick = { clearCanvas() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Стереть")
                    }
                }
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
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
    Dialog(onDismissRequest = onClose) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TsuWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Выберите достопримечательности")
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(landmarks.size) { index ->
                        val lm = landmarks[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
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
                            Text(lm.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TsuBlue,
                        contentColor = TsuWhite
                    )
                ) {
                    Text("Построить маршрут")
                }
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
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

    Dialog(onDismissRequest = onClose) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TsuWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Выберите, что купить")
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items.size) { index ->
                        val item = items[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
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
                }
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TsuBlue,
                        contentColor = TsuWhite
                    )
                ) { Text("Построить маршрут") }
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TsuBlue,
                        contentColor = TsuWhite
                    )
                ) { Text("Закрыть") }
            }
        }
    }
}

@Composable
fun DecisionTreeChatScreen(
    root: DecisionNode?,
    onExit: () -> Unit
) {
    data class ChatMsg(val text: String, val fromUser: Boolean)

    var currentNode by remember(root) { mutableStateOf(root) }
    var result by remember(root) { mutableStateOf<String?>(null) }
    var chat by remember(root) { mutableStateOf(listOf<ChatMsg>()) }


    LaunchedEffect(root) {
        chat = emptyList()
        result = null
        currentNode = root

        when (val node = currentNode) {
            is DecisionNode.Split -> {
                chat = chat + ChatMsg(featureRu(node.feature), fromUser = false)
            }

            is DecisionNode.Leaf -> {
                result = node.prediction
                chat = chat + ChatMsg("Рекомендованное место: ${node.prediction}", fromUser = false)
            }

            null -> {
                chat = chat + ChatMsg("Дерево не загружено", fromUser = false)
            }
        }
    }

    fun answer(value: String) {
        val node = currentNode
        if (node !is DecisionNode.Split) return


        chat = chat + ChatMsg(valueRu(value), fromUser = true)

        val next = node.children[value] ?: DecisionNode.Leaf(node.majorityLabel)
        currentNode = next

        when (next) {
            is DecisionNode.Split -> {
                chat = chat + ChatMsg(featureRu(next.feature), fromUser = false)
            }

            is DecisionNode.Leaf -> {
                result = next.prediction
                chat = chat + ChatMsg("Рекомендованное место: ${next.prediction}", fromUser = false)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TsuWhite)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TsuBlue)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Дерево решений", color = TsuWhite)
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TsuWhite,
                        contentColor = TsuBlue
                    )
                ) { Text("Выйти") }
            }


            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chat.size) { i ->
                    val msg = chat[i]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (msg.fromUser) TsuBlue else Color(0xFFEAEAEA),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = if (msg.fromUser) TsuWhite else Color.Black
                            )
                        }
                    }
                }
            }


            val options = (currentNode as? DecisionNode.Split)?.children?.keys?.sorted().orEmpty()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F3F3))
                    .navigationBarsPadding()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (options.isNotEmpty()) {
                    options.forEach { value ->
                        Button(
                            onClick = { answer(value) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TsuBlue,
                                contentColor = TsuWhite
                            )
                        ) {
                            Text(valueRu(value))
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            currentNode = root
                            result = null
                            chat = emptyList()
                            val node = currentNode
                            if (node is DecisionNode.Split) {
                                chat = chat + ChatMsg(featureRu(node.feature), fromUser = false)
                            } else if (node is DecisionNode.Leaf) {
                                chat = chat + ChatMsg(
                                    "Рекомендованное место: ${node.prediction}",
                                    fromUser = false
                                )
                            } else {
                                chat = chat + ChatMsg("Дерево не загружено", fromUser = false)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TsuBlue,
                            contentColor = TsuWhite
                        )
                    ) {
                        Text("Начать заново")
                    }
                }
            }
        }
    }
}


private fun featureRu(feature: String): String = when (feature) {
    "location" -> "Где вы находитесь?"
    "budget" -> "Какой у вас бюджет?"
    "time_available" -> "Сколько у вас времени?"
    "food_type" -> "Что хотите?"
    "queue_tolerance" -> "Готовы ли стоять в очереди?"
    "weather" -> "Какая погода?"
    else -> feature
}

private fun valueRu(value: String): String = when (value) {

    "sovetskaya" -> "Советская"
    "moskovskiy_trakt" -> "Московский тракт"
    "corpus_2" -> "2-й корпус"
    "lenina" -> "Ленина"
    "main_building" -> "Главный корпус"


    "low" -> "Низкий"
    "medium" -> "Средний"
    "high" -> "Высокий"


    "very_short" -> "Очень мало"
    "short" -> "Немного"

    "snack" -> "Перекус"
    "coffee" -> "Кофе"
    "full_meal" -> "Полноценный обед"


    "good" -> "Хорошая"
    "bad" -> "Плохая"

    else -> value
}

private fun placeRu(place: String): String = when (place) {
    "Пятерочка" -> "Пятерочка"
    "Бристоль" -> "Бристоль"
    "Гастроном НАШ" -> "Гастроном НАШ"
    "Ярче" -> "Ярче"
    "Абрикос" -> "Абрикос"
    "Безумно" -> "Безумно"
    "Мария-Ра" -> "Мария-Ра"
    "XO Bakery" -> "XO Bakery"
    "Буфет 2 корпус" -> "Буфет 2 корпус"
    "Вендинговый Автомат 2 корпус (2этаж)" -> "Вендинговый автомат 2 корпус (2 этаж)"
    "Гербарий" -> "Гербарий"
    "Rostiks" -> "Rostiks"
    "Сибирские блины (Ленина)" -> "Сибирские блины (Ленина)"
    "Сибирские блины (ЦК)" -> "Сибирские блины (ЦК)"
    "Минутка" -> "Минутка"
    "100ловая" -> "100ловая"
    "Вендинговый Автомат 1 корпус (1этаж)" -> "Вендинговый автомат 1 корпус (1 этаж)"
    else -> place
}