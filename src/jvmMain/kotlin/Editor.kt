import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val canvasWidth = 600.dp
private val columnWidth = 50.dp
private val columnShapeWidth = 20.dp

private val lineDp = 16.dp
private val startDotDp = 40.dp
private val paneDp = 60.dp

private val blackDotDp = 10.dp
private val paneShapeDp = 20.dp

val colorPuzzle = Color(0xFF, 0x80, 0x00)
private val selectColor = Color.Cyan
private val paneColor = Color(colorPuzzle.red, colorPuzzle.green, colorPuzzle.blue, 0x40 / (0xFF).toFloat())

private val squareShape = RoundedCornerShape(10)
private val lineBreakLength = 0.2f

@Composable
fun editor(puzzle: Puzzle, onClose: (Boolean) -> Unit) {
    Window(
        onCloseRequest = { onClose(false) },
        state = WindowState(size = DpSize(canvasWidth + columnWidth * 2 + 15.dp, canvasWidth - 1.dp),
            position = WindowPosition(Alignment.Center)
        ),
        title = "Puzzle Editor",
        resizable = false
    ) {
        // 'selected' change when mouse on an object
        val selectViewModel = remember { SelectViewModel() }

        // change puzzle when click on dot, line or pane
        val onClick = { onPuzzleClick(puzzle, selectViewModel) }

        val coloredComplexity = listOf(
            ComplexityType.Square,
            ComplexityType.Sun,
        )

        // true, if a user can put PuzzleComplexity on selected PuzzleObject
        fun isDrawSelecting(): Boolean {
            val complexityOnDot = listOf(
                ComplexityType.BlackDot,
            )
            val complexityOnLine = listOf(
                ComplexityType.BlackDot,
                ComplexityType.LineBreak,
            )
            val complexityOnPane = listOf(
                ComplexityType.Sun,
                ComplexityType.Square,
            )
            if (selectViewModel.selectedComplexity == null)
                return true
            return when(selectViewModel.selectedObj) {
                PuzzleObj.Dot -> selectViewModel.selectedComplexity in complexityOnDot
                PuzzleObj.Line -> selectViewModel.selectedComplexity in complexityOnLine
                PuzzleObj.Pane -> selectViewModel.selectedComplexity in complexityOnPane
                else -> false
            }
        }

        // trigger for selecting/unselecting coloredComplexity
        LaunchedEffect(selectViewModel.selectedColor, selectViewModel.selectedComplexity) {
            if (selectViewModel.selectedComplexity in coloredComplexity) {
                if (selectViewModel.selectedColor == null)
                    selectViewModel.selectedColor = PuzzleColor.Black
            } else {
                if (selectViewModel.selectedColor != null)
                    selectViewModel.selectedColor = null
            }
        }

        val showFullscreen = remember { mutableStateOf(false) }
        if (showFullscreen.value)
            fullscreenPuzzleView(puzzle) { showFullscreen.value = false }

        MaterialTheme {
            Row(Modifier.fillMaxSize()) {
                val colPadding = 4.dp
                // left column for selecting a complexity type
                Column(
                    Modifier.fillMaxHeight().width(50.dp).background(Color.Gray),
                    Arrangement.Top,
                    Alignment.CenterHorizontally,
                ) {
                    val setComplexity: (ComplexityType?) -> Unit = {
                        selectViewModel.selectedComplexity = it
                    }
                    box(colPadding, selectViewModel.selectedComplexity, setComplexity,
                        ComplexityType.BlackDot) {
                        Box(Modifier.size(columnShapeWidth.times(0.5f)).background(Color.Black, CircleShape))
                    }
                    box(colPadding, selectViewModel.selectedComplexity, setComplexity,
                        ComplexityType.LineBreak) {
                        Row(Modifier.fillMaxSize().scale(1f, 0.2f)) {
                            Spacer(Modifier.weight((1f - lineBreakLength)/2).fillMaxSize().background(colorPuzzle))
                            Spacer(Modifier.weight(lineBreakLength).fillMaxSize())
                            Spacer(Modifier.weight((1f - lineBreakLength)/2).fillMaxSize().background(colorPuzzle))
                        }
                    }
                    box(colPadding, selectViewModel.selectedComplexity, setComplexity,
                        ComplexityType.Sun) {
                        Box(Modifier.size(columnShapeWidth).background(Color.Black, SunShape()))
                    }
                    box(colPadding, selectViewModel.selectedComplexity, setComplexity,
                        ComplexityType.Square) {
                        Box(Modifier.size(columnShapeWidth).background(Color.White, RoundedCornerShape(20)))
                    }
                    Column(Modifier.fillMaxHeight(), Arrangement.Bottom) {
                        boxButton(colPadding, { showFullscreen.value = true }) {
                            Text("show")
                        }
                        boxButton(colPadding, {onClose(true)}) {
                            Text("save")
                        }
                        boxButton(colPadding, { onClose(false) }) {
                            Text("not save", textAlign = TextAlign.Center)
                        }
                    }
                }

                // canvas with puzzle
                Box(Modifier.size(canvasWidth).graphicsLayer { rotationX = 180f }) {
                    val onExit = {
                        if (isDrawSelecting().not())
                            selectViewModel.glowNone()
                    }
                    // puzzle dots, lines and panes
                    puzzle.paneMap.forEachIndexed { ind, (it, _) ->
                        drawShape(canvasWidth, it.x, it.y, paneDp, paneColor, RoundedCornerShape(20),
                            onEnter = { selectViewModel.glowPane(ind) }, onExit = onExit
                        )
                    }
                    puzzle.startDots.forEach { drawShape(canvasWidth, it.x, it.y, startDotDp, colorPuzzle) }
                    puzzle.lines.forEachIndexed { ind, it ->
                        drawLine(canvasWidth, it, lineDp, colorPuzzle,
                            onEnter = { selectViewModel.glowLine(ind) }, onExit = onExit
                        )
                    }
                    puzzle.dots.forEachIndexed { ind, it ->
                        drawShape(canvasWidth, it.x, it.y, lineDp, colorPuzzle,
                            onEnter = { selectViewModel.glowDot(ind) }, onExit = onExit
                        )
                    }

                    // selecting change color
                    if (isDrawSelecting())
                        when (selectViewModel.selectedObj) {
                            PuzzleObj.Dot -> {
                                val dot = puzzle.dots[selectViewModel.selectedInd]
                                drawShape(canvasWidth, dot.x, dot.y, lineDp, selectColor, RectangleShape,
                                    onExit = selectViewModel::glowNone, onClick = onClick
                                )
                            }

                            PuzzleObj.Line -> {
                                val line = puzzle.lines[selectViewModel.selectedInd]
                                val angle = atan2(line.dot2.y - line.dot1.y, line.dot2.x - line.dot1.x)
                                val length = lineDp.div(canvasWidth) / 2
                                val (dx, dy) = cos(angle) * length to sin(angle) * length
                                val newLine = Line(Dot(line.dot1.x + dx, line.dot1.y + dy), Dot(line.dot2.x - dx, line.dot2.y - dy))
                                drawLine(canvasWidth, newLine, lineDp, selectColor,
                                    onExit = selectViewModel::glowNone, onClick = onClick
                                )
                            }

                            PuzzleObj.Pane -> {
                                val pane = puzzle.paneMap[selectViewModel.selectedInd].first
                                drawShape(canvasWidth, pane.x, pane.y, paneDp, selectColor, RoundedCornerShape(20),
                                    onExit = selectViewModel::glowNone, onClick = onClick
                                )
                            }

                            else -> Unit
                        }

                    // complexity
                    puzzle.complexity.blackDotsOnDot.forEach {
                        drawShape(canvasWidth, it.x, it.y, blackDotDp, Color.Black,
                            onEnter = { selectViewModel.glowDot(puzzle.dots.indexOf(it)) },
                            onClick = onClick
                        )
                    }
                    puzzle.complexity.blackDotsOnLine.forEach {
                        val x = (it.dot1.x + it.dot2.x) / 2
                        val y = (it.dot1.y + it.dot2.y) / 2
                        drawShape(canvasWidth, x, y, blackDotDp, Color.Black,
                            onEnter = { selectViewModel.glowLine(puzzle.lines.indexOf(it)) },
                            onClick = onClick
                        )
                    }
                    puzzle.complexity.lineBreaks.forEach {
                        drawLine(canvasWidth, it, lineDp, Color.White, lengthScale = lineBreakLength,
                            onEnter = { selectViewModel.glowLine(puzzle.lines.indexOf(it)) },
                            onExit = selectViewModel::glowNone,
                            onClick = onClick
                        )
                    }
                    puzzle.complexity.suns.forEach {
                        drawShape(canvasWidth, it.pane.x, it.pane.y, paneShapeDp, it.color.color, SunShape(),
                            onEnter = { selectViewModel.glowPane(puzzle.paneMap.indexOfFirst { it2 -> it2.first == it.pane }) },
                            onClick = onClick
                        )
                    }
                    puzzle.complexity.squares.forEach {
                        drawShape(canvasWidth, it.pane.x, it.pane.y, paneShapeDp, it.color.color, squareShape,
                            onEnter = { selectViewModel.glowPane(puzzle.paneMap.indexOfFirst { it2 -> it2.first == it.pane }) },
                            onClick = onClick
                        )
                    }
                }

                Column(
                    Modifier.fillMaxHeight().width(50.dp).background(Color.Gray),
                    Arrangement.Top,
                    Alignment.CenterHorizontally,
                ) {
                    if (selectViewModel.selectedComplexity in coloredComplexity)
                        PuzzleColor.values().forEach {
                            val contrastColor = Color(Color.White.value - it.color.value + Color.Black.value)
                            val averVal = (contrastColor.blue + contrastColor.red + contrastColor.green) / 3
                            box(colPadding,selectViewModel.selectedColor, { selectViewModel.selectedColor = it },
                                it,
                                color = it.color,
                                selectColor = Color(averVal, averVal, averVal),
                                canUnselect = false) {}
                        }
                }
            }
        }
    }
}

private class SelectViewModel {
    var selectedInd by mutableStateOf(-1) // index of a selected object
    var selectedObj by mutableStateOf<PuzzleObj?>(null) // dot, line or pane
    var selectedComplexity by mutableStateOf<ComplexityType?>(null) // selected puzzle type
    var selectedColor by mutableStateOf<PuzzleColor?>(PuzzleColor.Black)

    fun glowObj(ind: Int, obj: PuzzleObj?) {
        selectedInd = ind
        selectedObj = obj
    }
    fun glowNone() = glowObj(-1, null)
    fun glowDot(ind: Int) = glowObj(ind, PuzzleObj.Dot)
    fun glowLine(ind: Int) = glowObj(ind, PuzzleObj.Line)
    fun glowPane(ind: Int) = glowObj(ind, PuzzleObj.Pane)

    fun redrawGlow() {
        selectedInd.also {
            selectedInd = -1
            selectedInd = it
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun <T> box(
    padding: Dp,
    selected: T?,
    selectFunc: (T?) -> Unit,
    selectType: T,
    canUnselect: Boolean = true,
    color: Color = Color.LightGray,
    selectColor: Color = Color.DarkGray,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        onClick = {
            if (selected != selectType) {
                selectFunc(selectType)
            } else {
                if (canUnselect)
                    selectFunc(null)
            }
        },
        modifier = Modifier.padding(padding).size(columnWidth - padding * 2),
        shape = RoundedCornerShape(20),
        color = if (selected == selectType) selectColor else color,
    ) {
        Box(
            Modifier
                .fillMaxWidth().padding(padding).size(columnWidth - padding * 2)
                .background(
                    if (selected == selectType) color else Color.Transparent,
                    RoundedCornerShape(20))
            , Alignment.Center, content = content
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun boxButton(
    padding: Dp,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.padding(padding).size(columnWidth - padding * 2),
        shape = RoundedCornerShape(20),
        color = Color.LightGray,
    ) {
        Box(Modifier.fillMaxSize(), Alignment.Center, content = content)
    }
}

private fun onPuzzleClick(
    puzzle: Puzzle,
    selectViewModel: SelectViewModel,
) {
    val complexity = puzzle.complexity

    when (selectViewModel.selectedObj) {
        PuzzleObj.Dot -> {
            val dot = puzzle.dots[selectViewModel.selectedInd]
            val contains = complexity.blackDotsOnDot.contains(dot)

            if (contains) {
                complexity.blackDotsOnDot.remove(dot)
            } else
                when (selectViewModel.selectedComplexity) {
                    ComplexityType.BlackDot -> { complexity.blackDotsOnDot.add(dot) }

                    else -> {}
                }
        }

        PuzzleObj.Line -> {
            val line = puzzle.lines[selectViewModel.selectedInd]
            val contains = complexity.blackDotsOnLine.contains(line) ||
                    complexity.lineBreaks.contains(line)

            if (contains) {
                complexity.blackDotsOnLine.remove(line)
                complexity.lineBreaks.remove(line)
            } else
                when (selectViewModel.selectedComplexity) {
                    ComplexityType.BlackDot -> { complexity.blackDotsOnLine.add(line) }
                    ComplexityType.LineBreak -> { complexity.lineBreaks.add(line) }

                    else -> {}
                }
        }

        PuzzleObj.Pane -> {
            val pane = puzzle.paneMap[selectViewModel.selectedInd].first
            val contains = complexity.suns.firstOrNull { it.pane == pane } != null ||
                    complexity.squares.firstOrNull { it.pane == pane } != null

            if (contains) {
                complexity.suns.removeIf { it.pane == pane }
                complexity.squares.removeIf { it.pane == pane }
            } else {
                when (selectViewModel.selectedComplexity) {
                    ComplexityType.Sun -> {
                        complexity.suns.removeIf { it.pane == pane }
                        complexity.suns.add(ColoredPane(pane, selectViewModel.selectedColor!!))
                    }

                    ComplexityType.Square -> {
                        complexity.squares.removeIf { it.pane == pane }
                        complexity.squares.add(ColoredPane(pane, selectViewModel.selectedColor!!))
                    }

                    else -> {}
                }
            }
        }
        else -> {}
    }
    selectViewModel.redrawGlow()
}

@Composable
fun drawPuzzle(
    puzzleItemDp: Dp,
    puzzleDrawDp: Dp,
    puzzle: Puzzle,
    backgroundColor: Color = Color.White,
) {
    Box(Modifier
        .scale(puzzleItemDp / puzzleDrawDp)
        .absoluteOffset((puzzleDrawDp - puzzleItemDp).div(-2), (puzzleDrawDp - puzzleItemDp).div(2))
    ) {
        Box(Modifier
            .fillMaxSize()
            .graphicsLayer { rotationX = 180f }
        ) {
            // puzzle
            puzzle.startDots.forEach { drawShape(puzzleDrawDp, it.x, it.y, startDotDp, colorPuzzle) }
            puzzle.lines.forEach { drawLine(puzzleDrawDp, it, lineDp, colorPuzzle) }
            puzzle.dots.forEach { drawShape(puzzleDrawDp, it.x, it.y, lineDp, colorPuzzle) }

            // complexity
            puzzle.complexity.blackDotsOnDot.forEach { drawShape(puzzleDrawDp, it.x, it.y, blackDotDp, Color.Black) }
            puzzle.complexity.blackDotsOnLine.forEach { drawShape(puzzleDrawDp, it.getX(), it.getY(), blackDotDp, Color.Black) }
            puzzle.complexity.lineBreaks.forEach { drawLine(puzzleDrawDp, it, lineDp, backgroundColor, lengthScale = lineBreakLength) }
            puzzle.complexity.suns.forEach { drawShape(puzzleDrawDp, it.pane.x, it.pane.y, 20.dp, it.color.color, SunShape()) }
            puzzle.complexity.squares.forEach { drawShape(puzzleDrawDp, it.pane.x, it.pane.y, 20.dp, it.color.color, squareShape) }
        }
    }
}

private enum class PuzzleObj {
    Line,
    Dot,
    Pane,
}
private enum class ComplexityType {
    BlackDot,
    LineBreak,
    Sun,
    Square,
}

@Composable
private fun fullscreenPuzzleView(puzzle: Puzzle, closeRequest: () -> Unit) {
    Window(
        onCloseRequest = closeRequest,
        state = WindowState(
            size = DpSize(600.dp, 600.dp),
            position = WindowPosition(Alignment.Center)
        ),
        resizable = false,
        title = "Puzzle",
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationX = 180f }) {
            // puzzle dots, lines and panes
            puzzle.startDots.forEach { drawShape(canvasWidth, it.x, it.y, startDotDp, colorPuzzle) }
            puzzle.lines.forEachIndexed { _, it ->
                drawLine(canvasWidth, it, lineDp, colorPuzzle)
            }
            puzzle.dots.forEachIndexed { _, it ->
                drawShape(canvasWidth, it.x, it.y, lineDp, colorPuzzle)
            }

            // complexity
            puzzle.complexity.blackDotsOnDot.forEach {
                drawShape(canvasWidth, it.x, it.y, blackDotDp, Color.Black)
            }
            puzzle.complexity.blackDotsOnLine.forEach {
                val x = (it.dot1.x + it.dot2.x) / 2
                val y = (it.dot1.y + it.dot2.y) / 2
                drawShape(canvasWidth, x, y, blackDotDp, Color.Black)
            }
            puzzle.complexity.lineBreaks.forEach {
                drawLine(canvasWidth, it, lineDp, Color.White, lengthScale = lineBreakLength)
            }
            puzzle.complexity.suns.forEach {
                drawShape(canvasWidth, it.pane.x, it.pane.y, paneShapeDp, it.color.color, SunShape())
            }
            puzzle.complexity.squares.forEach {
                drawShape(canvasWidth, it.pane.x, it.pane.y, paneShapeDp, it.color.color, squareShape)
            }
        }
    }
}
