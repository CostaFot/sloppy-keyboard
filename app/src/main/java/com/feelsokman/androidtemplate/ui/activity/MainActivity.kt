package com.feelsokman.androidtemplate.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.feelsokman.androidtemplate.R
import com.feelsokman.androidtemplate.retain.SampleRetainedViewModel
import com.feelsokman.androidtemplate.retain.rememberRetainDecorator
import com.feelsokman.androidtemplate.retain.rememberRetainedViewModel
import com.feelsokman.androidtemplate.ui.activity.viewmodel.MainViewModel
import com.feelsokman.androidtemplate.ui.activity.viewmodel.PullToRefreshViewModel
import com.feelsokman.common.NetworkMonitor
import com.feelsokman.design.theme.AppTheme
import com.feelsokman.logging.logDebug
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import javax.inject.Inject
import kotlin.random.Random

@Serializable
private data object RouteA : NavKey

@Serializable
private data class RouteB(val id: String) : NavKey

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    val rr = "test lint issues"

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        //installSplashScreen()
        super.onCreate(savedInstanceState)

        // Turn off the decor fitting system windows, which allows us to handle insets,
        // including IME animations, and go edge-to-edge
        // This also sets up the initial system bar style based on the platform theme
        // enableEdgeToEdge()

        setContent {
            AppTheme {
                val defaultSystemBarColor = Color.Transparent
                var systemBarStyle by remember {
                    mutableStateOf(
                        SystemBarStyle.dark(
                            defaultSystemBarColor.toArgb(),
                        )
                    )
                }
                DisposableEffect(systemBarStyle) {
                    enableEdgeToEdge(
                        statusBarStyle = systemBarStyle,
                        navigationBarStyle = systemBarStyle
                    )
                    onDispose {}
                }

                Surface {
                    val backStack = rememberNavBackStack(RouteA)

                    val retainDecorator = rememberRetainDecorator()

                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryDecorators = listOf(
                            retainDecorator,
                        ),
                        entryProvider = entryProvider {
                            entry<RouteA> {
                                MainScreen()
                            }
                            entry<RouteB> { key ->
                                ContentBlue("Route id: ${key.id} ")
                            }
                        }
                    )

                }
            }
        }
    }

    override fun onDestroy() {
        logDebug { "onDestroy" }
        super.onDestroy()
    }
}


@Composable
fun ContentGreen(goToSecondScreen: () -> Unit) {
    val retainedViewModel = rememberRetainedViewModel<SampleRetainedViewModel>()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Green)
            .safeDrawingPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "First screen",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            goToSecondScreen()

        }) {
            Text("Go to second screen")
        }
    }
}

@Composable
fun ContentBlue(
    text: String
) {
    val retainedViewModel = rememberRetainedViewModel<SampleRetainedViewModel>()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Blue)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun MainScreen(
    viewModel: PullToRefreshViewModel = hiltViewModel()
) {
    val ff = rememberRetainedViewModel<SampleRetainedViewModel>()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Blue)

    ) {
        val isRefreshing: Boolean by viewModel.isRefreshingState.collectAsStateWithLifecycle()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.onRefresh()
            },
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.statusBars.only(
                    WindowInsetsSides.Top
                )
            )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    var text by remember { mutableStateOf("type something") }
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("type something") },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ExpandingEdgeToEdge(
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { paddingValues ->

        val layoutDirection = LocalLayoutDirection.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    bottom = paddingValues.calculateBottomPadding(),
                )
        ) {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                var color by remember {
                    mutableStateOf(generateRandomColor)
                }
                LaunchedEffect(Unit) {
                    while (true) {
                        color = generateRandomColor
                        delay(1000)
                    }

                }

                val animatedColor by animateColorAsState(targetValue = color, label = "")

                val statusBarHeight =
                    WindowInsets.statusBars.getTop(LocalDensity.current).pxToDp()

                var isFullExpanded by remember {
                    mutableStateOf(false)
                }
                AnimatedVisibility(visible = isFullExpanded) {
                    Spacer(
                        modifier = Modifier
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                            .fillMaxWidth()
                            .background(animatedColor)
                    )
                }
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {

                    val minimumBoxHeight = 200.dp
                    val maximumBoxHeight =
                        maxHeight - statusBarHeight // stop before covering the status bar
                    var boxHeight by remember {
                        mutableStateOf(minimumBoxHeight)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(boxHeight)
                            .background(Color.White)
                            .align(Alignment.BottomCenter)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { change, dragAmount ->
                                    if (dragAmount < 0f) { // dragging up
                                        if (boxHeight >= maximumBoxHeight) {
                                            isFullExpanded = true
                                            boxHeight = maximumBoxHeight
                                        } else {
                                            if (boxHeight <= maxHeight - statusBarHeight) {
                                                boxHeight += 6.dp
                                            }
                                        }
                                    } else if (dragAmount > 0f) { // dragging down
                                        isFullExpanded = false
                                        if (boxHeight <= minimumBoxHeight) {
                                            boxHeight = minimumBoxHeight
                                        } else {
                                            boxHeight -= 6.dp
                                        }
                                    }
                                }
                            }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.cheems),
                            contentDescription = "null",
                            modifier = Modifier
                                .size(300.dp)
                                .align(Alignment.Center)
                        )

                    }
                }
            }


        }

    }
}

@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

@Composable
private fun InnerMainScreenContent() {


}


@Composable
fun SecondScreen(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val onUpdateState: () -> Unit = remember(viewModel) {
        return@remember viewModel::updateState
    }

    val onDoSomethingElse: () -> Unit = remember(viewModel) {
        return@remember viewModel::doSomethingElse
    }
    val state: String = "rfefe"
    SecondMainContent(
        state = state,
        onUpdateState = viewModel::updateState,
        onDoSomethingElse = viewModel::doSomethingElse,
    )
}

@Composable
fun SecondMainContent(
    state: String,
    onUpdateState: () -> Unit,
    onDoSomethingElse: () -> Unit
) {
    Column {
        TextThatDisplaysState(state = state)
        FirstComposable(onUpdateState = onUpdateState)
        SecondComposable(onDoSomethingElse = onDoSomethingElse)
    }
}

@Composable
fun TextThatDisplaysState(state: String) {
    Text(text = state)
}

@Composable
private fun FirstComposable(onUpdateState: () -> Unit) {
    var ff by remember {
        mutableStateOf("hello first")
    }
    Text(text = ff)
    Button(
        onClick = {
            onUpdateState()
        }
    ) {
        Text(text = "Click me to update state")
    }
}

@Composable
private fun SecondComposable(onDoSomethingElse: () -> Unit) {

    Text(text = "hello second")
    Button(
        onClick = {
            onDoSomethingElse()
        }
    ) {
        Text(text = "frewferf")
    }
}

@Composable
fun ThirdRouteScreen(
    viewModel: MainViewModel = hiltViewModel(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(Color.Red)
    ) {

    }
}

val generateRandomColor
    get() = Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))




