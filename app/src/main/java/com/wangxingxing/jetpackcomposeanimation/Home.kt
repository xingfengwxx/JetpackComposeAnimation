package com.wangxingxing.jetpackcomposeanimation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wangxingxing.jetpackcomposeanimation.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * author : 王星星
 * date : 2021/9/22 11:33
 * email : 1099420259@qq.com
 * description :
 */

private enum class TabPage {
    Home, Work
}

@Composable
fun Home() {
    // String resources.
    val allTasks = stringArrayResource(R.array.tasks)
    val allTopics = stringArrayResource(R.array.topics).toList()

    // The currently selected tab.
    var tabPage by remember { mutableStateOf(TabPage.Home) }

    // True if the whether data is currently loading.
    var weatherLoading by remember { mutableStateOf(false) }

    // Holds all the tasks currently shown on the task list.
    val tasks = remember { mutableStateListOf(*allTasks) }

    // Holds the topic that is currently expanded to show its body.
    var expandedTopic by remember { mutableStateOf<String?>(null) }

    // True if the message about the edit feature is shown.
    var editMessageShown by remember { mutableStateOf(false) }

    // Simulates loading weather data. This takes 3 seconds.
    suspend fun loadWeather() {
        if (!weatherLoading) {
            weatherLoading = true
            delay(3000L)
            weatherLoading = false
        }
    }

    suspend fun showEditMessage() {
        if (!editMessageShown) {
            editMessageShown = true
            delay(3000L)
            editMessageShown = false
        }
    }

    // Load the weather at the initial composition.
    LaunchedEffect(Unit) {
        loadWeather()
    }

    val lazyListState = rememberLazyListState()

    // The background color. The value is changed by the current tab.
    // TODO 1: Animate this color change.
    val backgroundColor by animateColorAsState(if (tabPage == TabPage.Home) Purple100 else Green300)

    // The coroutine scope for event handlers calling suspend functions.
    val coroutineScope = rememberCoroutineScope()
    Scaffold(topBar = {
        HomeTabBar(
            backgroundColor = backgroundColor,
            tabPage = tabPage,
            onTabSelected = { tabPage = it }
        )
    },
        backgroundColor = backgroundColor,
        floatingActionButton = {
            HomeFloatingActionButton(
                extended = lazyListState.isScrollingUp(),
                onClick = {
                    coroutineScope.launch {
                        showEditMessage()
                    }
                }
            )
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 32.dp),
            state = lazyListState
        ) {
            // Weather
            item { Header(title = stringResource(R.string.weather)) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp
                ) {
                    if (weatherLoading) {
                        LoadingRow()
                    } else {
                        WeatherRow(onRefresh = {
                            coroutineScope.launch {
                                loadWeather()
                            }
                        })
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }

            // Topics
            item { Header(title = stringResource(R.string.topics)) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            items(allTopics) { topic ->
                TopicRow(
                    topic = topic,
                    expanded = expandedTopic == topic,
                    onClick = {
                        expandedTopic = if (expandedTopic == topic) null else topic
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }

            // Tasks
            item { Header(title = stringResource(R.string.tasks)) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            if (tasks.isEmpty()) {
                item {
                    TextButton(onClick = { tasks.clear(); tasks.addAll(allTasks) }) {
                        Text(stringResource(R.string.add_tasks))
                    }
                }
            }
            items(count = tasks.size) { i ->
                val task = tasks.getOrNull(i)
                if (task != null) {
                    key(task) {
                        TaskRow(
                            task = task,
                            onRemove = { tasks.remove(task) }
                        )
                    }
                }
            }
        }
        EditMessage(editMessageShown)
    }
}

/**
 * Shows the floating action button.
 *
 * @param extended Whether the tab should be shown in its expanded state.
 */
// AnimatedVisibility is currently an experimental API in Compose Animation.
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun HomeFloatingActionButton(
    extended: Boolean,
    onClick: () -> Unit
) {
    // Use `FloatingActionButton` rather than `ExtendedFloatingActionButton` for full control on
    // how it should animate.
    FloatingActionButton(onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null
            )
            // Toggle the visibility of the content with animation.
            // TODO 2-1: Animate this visibility change.
            AnimatedVisibility(extended) {
                Text(
                    text = stringResource(R.string.edit),
                    modifier = Modifier
                        .padding(start = 8.dp, top = 3.dp)
                )
            }
        }
    }
}

/**
 * Shows a message that the edit feature is not available.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EditMessage(shown: Boolean) {
    // TODO 2-2: The message should slide down from the top on appearance and slide up on
    //           disappearance.
    // 消息从顶部滑入和滑出。
    // 自定义动画，将enter和exit参数添加到AnimatedVisibility可组合中。
    AnimatedVisibility(
        visible = shown,
        enter = slideInVertically(
            // 一个 lambda，它获取内容的完整高度并返回滑入的初始偏移量，默认情况下它返回 -fullHeight/2
            // 通过从 偏移量 -fullHeight 向下滑动到 0 来进入。
            initialOffsetY = { fullHeight -> -fullHeight },
            animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
        ),
        exit = slideOutVertically(
            // 一个 lambda，它获取内容的完整高度并返回滑出的目标偏移量，默认情况下它返回 fullHeight/2
            // 通过从偏移量 0 向上滑动到 -fullHeight 退出
            targetOffsetY = { fullHeight -> -fullHeight },
            animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.secondary,
            elevation = 4.dp
        ) {
            Text(
                text = stringResource(R.string.edit_message),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * Returns whether the lazy list is currently scrolling up.
 */
@Composable
private fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

/**
 * Shows the header label.
 *
 * @param title The title to be shown.
 */
@Composable
private fun Header(
    title: String
) {
    Text(
        text = title,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.h5
    )
}

/**
 * Shows a row for one topic.
 *
 * @param topic The topic title.
 * @param expanded Whether the row should be shown expanded with the topic body.
 * @param onClick Called when the row is clicked.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun TopicRow(topic: String, expanded: Boolean, onClick: () -> Unit) {
    TopicRowSpacer(visible = expanded)
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = 2.dp,
        onClick = onClick
    ) {
        // TODO 3: Animate the size change of the content.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            Row {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = topic,
                    style = MaterialTheme.typography.body1
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.lorem_ipsum),
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
    TopicRowSpacer(visible = expanded)
}

/**
 * Shows a separator for topics.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TopicRowSpacer(visible: Boolean) {
    AnimatedVisibility(visible = visible) {
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Shows the bar that holds 2 tabs.
 *
 * @param backgroundColor The background color for the bar.
 * @param tabPage The [TabPage] that is currently selected.
 * @param onTabSelected Called when the tab is switched.
 */
@Composable
private fun HomeTabBar(
    backgroundColor: Color,
    tabPage: TabPage,
    onTabSelected: (tabPage: TabPage) -> Unit
) {
    TabRow(
        selectedTabIndex = tabPage.ordinal,
        backgroundColor = backgroundColor,
        indicator = { tabPositions ->
            HomeTabIndicator(tabPosition = tabPositions, tabPage = tabPage)
        }
    ) {
        HomeTab(
            icon = Icons.Default.Home,
            title = stringResource(R.string.home),
            onClick = { onTabSelected(TabPage.Home) }
        )
        HomeTab(
            icon = Icons.Default.AccountBox,
            title = stringResource(R.string.work),
            onClick = { onTabSelected(TabPage.Work) }
        )
    }
}

/**
 * Shows an indicator for the tab.
 *
 * @param tabPositions The list of [TabPosition]s from a [TabRow].
 * @param tabPage The [TabPage] that is currently selected.
 */
@Composable
private fun HomeTabIndicator(
    tabPosition: List<TabPosition>,
    tabPage: TabPage
) {
    // TODO 4: Animate these value changes.
    // 多值动画，自定义选项卡指示器
    val transition = updateTransition(
        tabPage,
        label = "Tab indicator"
    )

    /**
    创建 Dp 动画作为给定Transition的一部分。这意味着此动画的状态将由 Transition 管理。
    targetValueByState 用作从目标状态到此动画的目标值的映射。 Transition 将使用此映射来确定此动画的目标值。
    请注意， targetValueByState 是一个可组合的函数。这意味着映射函数可以访问状态、CompositionLocals、主题等。如果 targetValue 在 Transition 运行之外发生变化（即当 Transition 已经达到其 targetState 时），Transition 将再次开始运行以确保此动画顺利到达其新目标.
    可以提供一个可选的 transitionSpec 来为每对 initialState 和 targetState 指定（可能不同的）动画。 FiniteAnimationSpec 包括任何非无限动画，例如补间、弹簧、关键帧甚至可重复，但不是无限可重复的。默认情况下，transitionSpec 对所有过渡目标使用弹簧动画。
    label 用于在 Android Studio 中与同一Transition中的其他动画区分开来。
     */
    val indicatorLeft by transition.animateDp(
        transitionSpec = {
            // 指标向右移动，左边缘比右边缘移动得慢。
            if (TabPage.Home isTransitioningTo TabPage.Work) {
                spring(stiffness = Spring.StiffnessVeryLow)
            } else {
                // 指标向左移动，左边缘比右边缘移动得更快。
                spring(stiffness = Spring.StiffnessMedium)
            }
        },
        label = "Indicator left"
    ) { tabPage ->
        tabPosition[tabPage.ordinal].left
    }

    val indicationRight by transition.animateDp(
        transitionSpec = {
            // 指标向右移动，右边缘比左边缘移动得更快。
            if (TabPage.Home isTransitioningTo TabPage.Work) {
                spring(stiffness = Spring.StiffnessMedium)
            } else {
                // 指标向左移动，右边缘比左边缘移动得慢。
                spring(stiffness = Spring.StiffnessVeryLow)
            }
        },
        label = "Indicator right"
    ) { tabPage ->
        tabPosition[tabPage.ordinal].right
    }

    val color by transition.animateColor(
        label = "Border color"
    ) { tabPage ->
        if (tabPage == TabPage.Home) Purple700 else Green800
    }

    Box(
        Modifier
            .fillMaxSize()
            .wrapContentSize(align = Alignment.BottomStart)
            .offset(x = indicatorLeft)
            .width(indicationRight - indicatorLeft)
            .padding(4.dp)
            .fillMaxSize()
            .border(
                BorderStroke(2.dp, color),
                RoundedCornerShape(4.dp)
            )
    )
}

/**
 * Shows a tab.
 *
 * @param icon The icon to be shown on this tab.
 * @param title The title to be shown on this tab.
 * @param onClick Called when this tab is clicked.
 * @param modifier The [Modifier].
 */
@Composable
private fun HomeTab(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title)
    }
}

/**
 * Shows the weather.
 *
 * @param onRefresh Called when the refresh icon button is clicked.
 */
@Composable
private fun WeatherRow(
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .heightIn(min = 64.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Amber600)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = stringResource(R.string.temperature), fontSize = 24.sp)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.refresh)
            )
        }
    }
}

/**
 * Shows the loading state of the weather.
 */
@Composable
private fun LoadingRow() {
    // TODO 5: Animate this value between 0f and 1f, then back to 0f repeatedly.
    // 重复动画（英菲尼迪）
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.7f at 500 // 添加一个关键帧，指定动画值在某个时间为某个值
            },
            // 动画应该从头开始（即RepeatMode.Restart）
            // 还是从尾开始（即RepeatMode.Reverse）重复
            repeatMode = RepeatMode.Reverse
        )
    )
    Row(
        modifier = Modifier
            .heightIn(min = 64.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(Color.LightGray.copy(alpha = alpha))
        )
    }
}

/**
 * Shows a row for one task.
 *
 * @param task The task description.
 * @param onRemove Called when the task is swiped away and removed.
 */
@Composable
private fun TaskRow(task: String, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .swipeToDismiss(onRemove),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = task,
                style = MaterialTheme.typography.body1
            )
        }
    }
}

/**
 * The modified element can be horizontally swiped away.
 *
 * @param onDismissed Called when the element is swiped to the edge of the screen.
 */
private fun Modifier.swipeToDismiss(
    onDismissed: () -> Unit
): Modifier = composed {
    // TODO 6-1: Create an Animatable instance for the offset of the swiped element.
    // 创建一个实例Animatable并使用它来表示可滑动元素的水平偏移。
    val offsetX = remember { Animatable(0f) }
    pointerInput(Unit) {
        // Used to calculate a settling position of a fling animation.
        val decay = splineBasedDecay<Float>(this)
        // Wrap in a coroutine scope to use suspend functions for touch events and animation.
        coroutineScope {
            while (true) {
                // Wait for a touch down event.
                val pointerId = awaitPointerEventScope { awaitFirstDown().id }
                // TODO 6-2: Touch detected; the animation should be stopped.
                // 如果动画当前正在运行，我们应该拦截它。这可以通过调用来完成stop的Animatable。
                offsetX.stop()
                // Prepare for drag events and record velocity of a fling.
                val velocityTracker = VelocityTracker()
                // Wait for drag events.
                awaitPointerEventScope {
                    horizontalDrag(pointerId) { change ->
                        // TODO 6-3: Apply the drag change to the Animatable offset.
                        // 我们不断收到拖动事件。我们必须将触摸事件的位置同步到动画值中。为此，我们可以使用snapTo on Animatable。
                        val horizontalDragOffset = offsetX.value + change.positionChange().x
                        launch {
                            offsetX.snapTo(horizontalDragOffset)
                        }
                        // Record the velocity of the drag.
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        // Consume the gesture event, not passed to external
                        change.consumePositionChange()
                    }
                }
                // Dragging finished. Calculate the velocity of the fling.
                val velocity = velocityTracker.calculateVelocity().x
                // TODO 6-4: Calculate the eventual position where the fling should settle
                //           based on the current offset value and velocity
                // 元素刚刚被释放和抛出的地方。
                // 我们需要计算投掷的最终位置，以决定是将元素滑回原始位置，还是将其滑开并调用回调。
                val targetOffsetX = decay.calculateTargetValue(offsetX.value, velocity)
                // TODO 6-5: Set the upper and lower bounds so that the animation stops when it
                //           reaches the edge.
                // 我们即将开始动画。但在此之前，我们要为 设置上限和下限，Animatable以便它在到达它们时立即停止。
                // 该pointerInput修改允许我们通过访问该元素的大小size属性，所以我们就用它，让我们的边界。
                offsetX.updateBounds(
                    lowerBound = -size.width.toFloat(),
                    upperBound = size.width.toFloat()
                )
                launch {
                    // TODO 6-6: Slide back the element if the settling position does not go beyond
                    //           the size of the element. Remove the element if it does.
                    // 我们最终可以开始我们的动画的地方。我们首先比较我们之前计算的fling的沉降位置和元素的大小。
                    // 如果沉降位置在尺寸以下，则说明抛掷速度不够。我们可以使用animateTo动画将值恢复为 0f。
                    // 否则，我们使用animateDecay来启动投掷动画。当动画完成时（很可能是我们之前设置的边界），我们可以调用回调。
                    if (targetOffsetX.absoluteValue <= size.width) {
                        // 速度不够； 滑回来。
                        offsetX.animateTo(targetValue = 0f, initialVelocity = velocity)
                    } else {
                        // 足够的速度将元素滑到边缘。
                        offsetX.animateDecay(velocity, decay)
                        // 元素被刷掉了。
                        onDismissed()
                    }
                }
            }
        }
    }.offset {
        // TODO 6-7: Use the animating offset value here.
        // 我们已经设置了所有动画和手势，所以不要忘记将偏移应用于元素。
        IntOffset(offsetX.value.roundToInt(), 0)
    }
}

@Preview
@Composable
private fun PreviewHomeTabBar() {
    HomeTabBar(
        backgroundColor = Purple200,
        tabPage = TabPage.Home,
        onTabSelected = {}
    )
}

@Preview
@Composable
private fun PreviewHome() {
    JetpackComposeAnimationTheme {
        Home()
    }
}

