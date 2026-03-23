package com.example.tsumap


import androidx.compose.ui.graphics.Color
import android.os.Bundle
import androidx.compose.material3.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.ShortcutInfoCompat
import com.example.tsumap.ui.theme.TsuBlue
import com.example.tsumap.ui.theme.TsuBlueLight
import com.example.tsumap.ui.theme.TsuWhite
import kotlin.collections.plusAssign
import kotlin.times


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

@Composable
fun MainMapScreen(){
    var scale by remember { mutableStateOf(2.5f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var state = rememberTransformableState {
            zoomChange, offsetChange, _ ->
        val proposedScale = scale * zoomChange
        scale = proposedScale.coerceIn(1f,5f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    Scaffold(bottomBar = { AlgorithmButtonsGrid()}) {
        paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TsuBlueLight)
                .padding(paddingValues)
                .clip(RectangleShape)
                .background(TsuBlueLight)
                .transformable(state = state)
        ){
            Image(
                painter = painterResource(id = R.drawable.tsu_map),
                contentDescription = "TSU map",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.None
            )
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