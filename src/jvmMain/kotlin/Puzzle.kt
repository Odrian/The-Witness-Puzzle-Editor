import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.serialization.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Puzzle(
    var id: Int,
    var startDots: MutableList<Dot>,
    var endDots: MutableList<Dot>,
    var dots: MutableList<Dot>,
    var lines: MutableList<Line>,
    var paneMap: MutableList<Pair<Pane, List<Pair<Pane, Line>>>>,
    var complexity: Complexity,
)

@Serializable
data class Dot(var x: Float, var y: Float)

data class Line(var dot1: Dot, var dot2: Dot)

@Serializable
data class Pane(var x: Float, var y: Float)

data class Complexity(
    var blackDotsOnDot: MutableList<Dot>,
    var blackDotsOnLine: MutableList<Line>,
    var suns: MutableList<ColoredPane>,
    var squares: MutableList<ColoredPane>,
) {
    companion object {
        fun empty() = Complexity(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
    }
}

data class ColoredPane(val pane: Pane, val color: PuzzleColor)

@Suppress("unused")
enum class PuzzleColor(val string: String, val color: Color) {
    Black("black", Color.Black),
    White("white", Color.White),
    Red("red", Color.Red),
    Green("green", Color.Green),
    Blue("blue", Color.Blue),
}

fun Line.getX() = (dot1.x + dot2.x) / 2
fun Line.getY() = (dot1.y + dot2.y) / 2

// custom sun shape
class SunShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val scale = 1.3f
        val mid = size.width / 2
        val radius = floatArrayOf(size.width / 2 * scale, size.width / 3 * scale)
        val nPoints = 2 * 8
        val pathDots = mutableListOf<Pair<Float, Float>>()

        for (current in 0 until nPoints) {
            val i: Int = current
            val y = -cos(current * (2 * Math.PI / nPoints)) * radius[i % radius.size]
            val x = sin(current * (2 * Math.PI / nPoints)) * radius[i % radius.size]
            pathDots.add((x.toFloat() + mid) to (y.toFloat() + mid))
        }
        val path = Path()
        pathDots.last().also { path.moveTo(it.first, it.second) }
        pathDots.forEach {
            path.lineTo(it.first, it.second)
        }

        return Outline.Generic(path)
    }
}

// draw rect like shape
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun drawShape(boxSize: Dp, x: Float, y: Float, size: Dp, color: Color, shape: Shape = CircleShape,
              onEnter: () -> Unit = {}, onExit: () -> Unit = {}, onClick: () -> Unit = {}) =
    Spacer(Modifier
        .absoluteOffset(boxSize * x, boxSize * y)
        .absoluteOffset(size.div(-2), size.div(-2))
        .size(size)
        .background(color, shape)
        .onPointerEvent(PointerEventType.Enter) { onEnter() }
        .onPointerEvent(PointerEventType.Exit) { onExit() }
        .onClick(onClick = onClick))

// draw line shape
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun drawLine(boxSize: Dp, line: Line, size: Dp, color: Color,
             onEnter: () -> Unit = {}, onExit: () -> Unit = {}, onClick: () -> Unit = {}) {
    val dx = line.dot2.x - line.dot1.x
    val dy = line.dot2.y - line.dot1.y
    val length = sqrt(dx * dx + dy * dy)
    val angle = Math.toDegrees(atan2(dx, dy).toDouble()).toFloat()

    Spacer(Modifier
        .absoluteOffset(
            boxSize * line.getX(),
            boxSize * line.getY())
        .absoluteOffset(size.div(-2), size.div(-2))
        .size(size)
        .rotate(-angle + 90f)
        .scale(length * (boxSize.div(size)), 1f)
        .background(color, RectangleShape)
        .onPointerEvent(PointerEventType.Enter) { onEnter() }
        .onPointerEvent(PointerEventType.Exit) { onExit() }
        .onClick(onClick = onClick))
}
