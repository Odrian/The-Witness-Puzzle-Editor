
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
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

@Composable
fun editor(puzzle: Puzzle, onClose: (Boolean) -> Unit) {
    Window(
        onCloseRequest = { onClose(false) },
        state = WindowState(size = DpSize(canvasWidth + columnWidth * 2 + 15.dp, canvasWidth),
            position = WindowPosition(Alignment.Center)
        ),
        title = "Puzzle Editor",
        resizable = false
    ) {
        // 'selected' change when mouse on an object
        val selectedInd = remember { mutableStateOf(-1) } // index of a selected object
        val selectedObj = remember { mutableStateOf<PuzzleObj?>(null) } // dot, line or pane
        val selectedComplexity = remember { mutableStateOf<ComplexityType?>(null) } // selected puzzle type
        val selectedColor = remember { mutableStateOf<PuzzleColor?>(PuzzleColor.Black) }

        // lambdas for select when mouse on dot, line or pane
        val selectObj = { ind: Int, obj: PuzzleObj? ->
            selectedObj.value = obj
            selectedInd.value = ind
        }
        val selectDot = { ind: Int -> selectObj(ind, PuzzleObj.Dot) }
        val selectLine = { ind: Int -> selectObj(ind, PuzzleObj.Line) }
        val selectPane = { ind: Int -> selectObj(ind, PuzzleObj.Pane) }
        val selectNone = { selectObj(-1, null) }
        val redrawSelect = { selectedInd.value.also { selectedInd.value = -1; selectedInd.value = it } }

        // change puzzle when click on dot, line or pane
        val onClick = fun() {
            if (selectedObj.value == null)
                return
            onPuzzleClick(puzzle, selectedObj.value!!, selectedComplexity.value, selectedInd.value, selectedColor.value)
            redrawSelect()
        }
        val coloredComplexity = listOf(
            ComplexityType.Square,
            ComplexityType.Sun,
        )

        // trigger for selecting/unselecting coloredComplexity
        LaunchedEffect(selectedColor.value, selectedComplexity.value) {
            if (selectedComplexity.value in coloredComplexity) {
                if (selectedColor.value == null)
                    selectedColor.value = PuzzleColor.Black
            } else {
                if (selectedColor.value != null)
                    selectedColor.value = null
            }
        }
        MaterialTheme {
            Row(Modifier.fillMaxSize()) {
                val colPadding = 4.dp
                // left column for selecting a complexity type
                Column(
                    Modifier.fillMaxHeight().width(50.dp).background(Color.Gray),
                    Arrangement.Top,
                    Alignment.CenterHorizontally,
                ) {
                    box(colPadding, selectedComplexity,
                        ComplexityType.BlackDot) {
                        Box(Modifier.size(columnShapeWidth.times(0.5f)).background(Color.Black, CircleShape))
                    }
                    box(colPadding, selectedComplexity,
                        ComplexityType.Sun) {
                        Box(Modifier.size(columnShapeWidth).background(Color.Black, SunShape()))
                    }
                    box(colPadding, selectedComplexity,
                        ComplexityType.Square) {
                        Box(Modifier.size(columnShapeWidth).background(Color.White, RoundedCornerShape(20)))
                    }
                    Column(Modifier.fillMaxHeight(), Arrangement.Bottom) {
                        boxButton(colPadding, {onClose(true)}) {
                            Text("save")
                        }
                        boxButton(colPadding, { onClose(false) }) {
                            Text("not save")
                        }
                    }
                }

                // canvas with puzzle
                Box(Modifier.size(canvasWidth).graphicsLayer { rotationX = 180f }) {
                    // puzzle dots, lines and panes
                    puzzle.paneMap.forEachIndexed { ind, (it, _) ->
                        drawShape(canvasWidth, it.x, it.y, paneDp, paneColor, RoundedCornerShape(20),
                            onEnter = { selectPane(ind) }
                        )
                    }
                    puzzle.startDots.forEach { drawShape(canvasWidth, it.x, it.y, startDotDp, colorPuzzle) }
                    puzzle.lines.forEachIndexed { ind, it ->
                        drawLine(canvasWidth, it, lineDp, colorPuzzle,
                            onEnter = { selectLine(ind) }
                        )
                    }
                    puzzle.dots.forEachIndexed { ind, it ->
                        drawShape(canvasWidth, it.x, it.y, lineDp, colorPuzzle,
                            onEnter = { selectDot(ind) }
                        )
                    }

                    // selecting change color
                    when (selectedObj.value) {
                        PuzzleObj.Dot -> {
                            val dot = puzzle.dots[selectedInd.value]
                            drawShape(canvasWidth, dot.x, dot.y, lineDp, selectColor, RectangleShape,
                                onExit = selectNone, onClick = onClick
                            )
                        }

                        PuzzleObj.Line -> {
                            val line = puzzle.lines[selectedInd.value]
                            val angle = atan2(line.dot2.y - line.dot1.y, line.dot2.x - line.dot1.x)
                            val length = lineDp.div(canvasWidth) / 2
                            val (dx, dy) = cos(angle) * length to sin(angle) * length
                            val newLine = Line(Dot(line.dot1.x + dx, line.dot1.y + dy), Dot(line.dot2.x - dx, line.dot2.y - dy))
                            drawLine(canvasWidth, newLine, lineDp, selectColor,
                                onExit = selectNone, onClick = onClick
                            )
                        }

                        PuzzleObj.Pane -> {
                            val pane = puzzle.paneMap[selectedInd.value].first
                            drawShape(canvasWidth, pane.x, pane.y, paneDp, selectColor, RoundedCornerShape(20),
                                onExit = selectNone, onClick = onClick
                            )
                        }

                        else -> Unit
                    }

                    // complexity
                    puzzle.complexity.blackDotsOnDot.forEachIndexed { ind, it ->
                        drawShape(canvasWidth, it.x, it.y, blackDotDp, Color.Black,
                            onEnter = { selectDot(puzzle.dots.indexOf(it)) },
                            onClick = { puzzle.complexity.blackDotsOnDot.removeAt(ind); redrawSelect() }
                        )
                    }
                    puzzle.complexity.blackDotsOnLine.forEachIndexed { ind, it ->
                        val x = (it.dot1.x + it.dot2.x) / 2
                        val y = (it.dot1.y + it.dot2.y) / 2
                        drawShape(canvasWidth, x, y, blackDotDp, Color.Black,
                            onEnter = { selectLine(puzzle.lines.indexOf(it)) },
                            onClick = { puzzle.complexity.blackDotsOnLine.removeAt(ind); redrawSelect() }
                        )
                    }
                    puzzle.complexity.suns.forEachIndexed { ind, it ->
                        drawShape(canvasWidth, it.pane.x, it.pane.y, paneShapeDp, it.color.color, SunShape(),
                            onEnter = { selectPane(puzzle.paneMap.indexOfFirst { it2 -> it2.first == it.pane }) },
                            onClick = { puzzle.complexity.suns.removeAt(ind); redrawSelect() }
                        )
                    }
                    puzzle.complexity.squares.forEachIndexed { ind, it ->
                        drawShape(canvasWidth, it.pane.x, it.pane.y, paneShapeDp, it.color.color, squareShape,
                            onEnter = { selectPane(puzzle.paneMap.indexOfFirst { it2 -> it2.first == it.pane }) },
                            onClick = { puzzle.complexity.squares.removeAt(ind); redrawSelect() }
                        )
                    }
                }

                Column(
                    Modifier.fillMaxHeight().width(50.dp).background(Color.Gray),
                    Arrangement.Top,
                    Alignment.CenterHorizontally,
                ) {
                    if (selectedComplexity.value in coloredComplexity)
                        PuzzleColor.values().forEach {
                            val contrastColor = Color(Color.White.value - it.color.value + Color.Black.value)
                            val averVal = (contrastColor.blue + contrastColor.red + contrastColor.green) / 3
                            box(colPadding, selectedColor, it,
                                color = it.color,
                                selectColor = Color(averVal, averVal, averVal),
                                canUnselect = false) {}
                        }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun <T> box(
    padding: Dp,
    selected: MutableState<T?>,
    select: T,
    canUnselect: Boolean = true,
    color: Color = Color.LightGray,
    selectColor: Color = Color.DarkGray,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        onClick = {
            if (selected.value != select) {
                selected.value = select
            } else {
                if (canUnselect)
                    selected.value = null
            }
        },
        modifier = Modifier.padding(padding).size(columnWidth - padding * 2),
        shape = RoundedCornerShape(20),
        color = if (selected.value == select) selectColor else color,
    ) {
        Box(
            Modifier
                .fillMaxWidth().padding(padding).size(columnWidth - padding * 2)
                .background(
                    if (selected.value == select) color else Color.Transparent,
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
    selectedObj: PuzzleObj,
    selectedComplexity: ComplexityType?,
    selectedInd: Int,
    selectedColor: PuzzleColor?,
) {
    val complexity = puzzle.complexity

    when (selectedObj) {
        PuzzleObj.Dot -> {
            val dot = puzzle.dots[selectedInd]
            val contains = complexity.blackDotsOnDot.contains(dot)

            if (contains) {
                complexity.blackDotsOnDot.remove(dot)
            } else
                when (selectedComplexity) {
                    ComplexityType.BlackDot -> { complexity.blackDotsOnDot.add(dot) }

                    else -> {}
                }
        }

        PuzzleObj.Line -> {
            val line = puzzle.lines[selectedInd]
            val contains = complexity.blackDotsOnLine.contains(line)

            if (contains) {
                complexity.blackDotsOnLine.remove(line)
            } else
                when (selectedComplexity) {
                    ComplexityType.BlackDot -> { complexity.blackDotsOnLine.add(line) }

                    else -> {}
                }
        }

        PuzzleObj.Pane -> {
            val pane = puzzle.paneMap[selectedInd].first
            fun ColoredPane?.isSame(): Boolean {
                if (this == null)
                    return false
                return this.color == selectedColor
            }
            val contains = complexity.suns.firstOrNull { it.pane == pane }.isSame() ||
                        complexity.squares.firstOrNull { it.pane == pane }.isSame()
            println(complexity.suns.firstOrNull { it.pane == pane }.isSame())

            if (contains) {
                complexity.suns.removeIf { it.pane == pane }
                complexity.squares.removeIf { it.pane == pane }
            } else {
                when (selectedComplexity) {
                    ComplexityType.Sun -> {
                        complexity.suns.removeIf { it.pane == pane }
                        complexity.suns.add(ColoredPane(pane, selectedColor!!))
                    }

                    ComplexityType.Square -> {
                        complexity.squares.add(ColoredPane(pane, selectedColor!!))
                        complexity.squares.removeIf { it.pane == pane }
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
fun drawPuzzle(boxSize: Dp, puzzle: Puzzle) {
    Box(Modifier.fillMaxSize().graphicsLayer { rotationX = 180f }) {
        // puzzle
        puzzle.startDots.forEach { drawShape(boxSize, it.x, it.y, startDotDp, colorPuzzle) }
        puzzle.lines.forEach { drawLine(boxSize, it, lineDp, colorPuzzle) }
        puzzle.dots.forEach { drawShape(boxSize, it.x, it.y, lineDp, colorPuzzle) }

        // complexity
        puzzle.complexity.blackDotsOnDot.forEach { drawShape(boxSize, it.x, it.y, blackDotDp, Color.Black) }
        puzzle.complexity.blackDotsOnLine.forEach { drawShape(boxSize, it.getX(), it.getY(), blackDotDp, Color.Black) }
        puzzle.complexity.suns.forEach { drawShape(boxSize, it.pane.x, it.pane.y, 20.dp, it.color.color, SunShape()) }
        puzzle.complexity.squares.forEach { drawShape(boxSize, it.pane.x, it.pane.y, 20.dp, it.color.color, squareShape) }
    }
}

private enum class PuzzleObj {
    Line,
    Dot,
    Pane,
}
private enum class ComplexityType {
    BlackDot,
    Sun,
    Square,
}
