package json

import ColoredPane
import Complexity
import Dot
import Line
import Pane
import Puzzle
import PuzzleColor
import kotlinx.serialization.Serializable

/*
    in json it changes most objects to indexes of it in a list
    in ue5 it will be more effective, because ue5 works badly with nested arrays

    createSimplePuzzle()
    as Puzzle in json takes 8.2 KB
    as PuzzleJson in json takes 4.8 KB

    actually, I think it doesn't matter at all

    great idea:
    save a few puzzle samples without complexity,
    and each puzzle will save only a sample link and complexity
 */

@Serializable
internal data class LineJson(val dot1: Int, val dot2: Int)

@Serializable
internal data class PaneJson(val ind: Int)

@Serializable
internal data class ColoredPaneJson(
    val ind: Int,
    val color: String
)

// default values need for ignore empty field, for example, when app updates and add new field
@Serializable
internal data class ComplexityJson(
    var blackDotsOnDot: List<Int>? = null,
    var blackDotsOnLine: List<LineJson>? = null,
    var lineBreaks: List<LineJson>? = null,
    var suns: List<ColoredPaneJson>? = null,
    var squares: List<ColoredPaneJson>? = null,
) {
    fun removeNulls() {
        if (blackDotsOnDot == null)
            blackDotsOnDot = listOf()
        if (blackDotsOnLine == null)
            blackDotsOnLine = listOf()
        if (lineBreaks == null)
            lineBreaks = listOf()
        if (suns == null)
            suns = listOf()
        if (squares == null)
            squares = listOf()
    }
}

@Serializable
internal data class PuzzleJson(
    val name: Int = -1, // for backward compatibility
    val startDots: List<Int>,
    val endDots: List<Int>,
    val dots: MutableList<Dot>,
    val lines: List<LineJson>,
    val panes: List<Pane>,
    val paneMap: List<Pair<PaneJson, List<Pair<PaneJson, LineJson>>>>,
    val complexityJson: ComplexityJson,
)

internal fun Puzzle.toPuzzleJson(): PuzzleJson {
    val panes = paneMap.map {it.first }
    fun Line.toLineJson() =
        LineJson(dots.indexOf(this.dot1), dots.indexOf(this.dot2))
    fun Pane.toPaneJson() =
        PaneJson(panes.indexOf(this))
    fun ColoredPane.toColoredPaneJson() =
        ColoredPaneJson(panes.indexOf(this.pane), this.color.string)

    return PuzzleJson(
        id,
        startDots.map { dots.indexOf(it) },
        endDots.map { dots.indexOf(it) },
        dots,
        lines.map { it.toLineJson() },
        panes,
        paneMap.map { it.first.toPaneJson() to it.second.map {
            it2 -> it2.first.toPaneJson() to it2.second.toLineJson() } },
        ComplexityJson(
            complexity.blackDotsOnDot.map { dots.indexOf(it) },
            complexity.blackDotsOnLine.map { it.toLineJson() },
            complexity.lineBreaks.map { it.toLineJson() },
            complexity.suns.map { it.toColoredPaneJson() },
            complexity.squares.map { it.toColoredPaneJson() },
        )
    )
}

internal fun PuzzleJson.toPuzzle(): Puzzle {
    fun LineJson.toLine() =
        Line(dots[this.dot1], dots[this.dot2])
    fun PaneJson.toPane() =
        panes[this.ind]
    fun ColoredPaneJson.toColoredPane() =
        ColoredPane(panes[this.ind], PuzzleColor.values().firstOrNull { it.string == this.color } ?: PuzzleColor.White)

    return Puzzle(
        name,
        startDots.map { dots[it] }.toMutableList(),
        endDots.map { dots[it] }.toMutableList(),
        dots.toMutableList(),
        lines.map { it.toLine() }.toMutableList(),
        paneMap.map { panes[it.first.ind] to it.second.map { it2 -> it2.first.toPane() to it2.second.toLine() }.toMutableList()}.toMutableList(),
        Complexity(
            complexityJson.blackDotsOnDot!!.map { dots[it] }.toMutableList(),
            complexityJson.blackDotsOnLine!!.map { it.toLine() }.toMutableList(),
            complexityJson.lineBreaks!!.map { it.toLine() }.toMutableList(),
            complexityJson.suns!!.map { it.toColoredPane() }.toMutableList(),
            complexityJson.squares!!.map { it.toColoredPane() }.toMutableList(),
        )
    )
}
