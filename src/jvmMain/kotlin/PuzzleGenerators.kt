fun createSimpleRectPuzzle(count: Int = 5, padding: Float = 0.18f): Puzzle {
    val dist = (1f - 2 * padding) / (count - 1)
    val dotByCord = { x: Float, y: Float -> Dot(padding + dist * x, padding + dist * y) }
    val dots2d = Array(count) { x -> Array(count) { y -> dotByCord(x.toFloat(), y.toFloat()) } }

    val xLines = Array(count-1) { x -> Array(count) {y -> Line(dots2d[x][y], dots2d[x+1][y])} }
    val yLines = Array(count) { x -> Array(count-1) {y -> Line(dots2d[x][y], dots2d[x][y+1])} }

    val allLines = xLines.flatten().plus(yLines.flatten()).toMutableList()
    val allDots = dots2d.flatten().toMutableList()

    val endDot = dotByCord(count-1f + .5f, count-1f)
    allDots.add(endDot)
    val endLine = Line(dots2d.last().last(), endDot)
    allLines.add(endLine)

    val complexity = Complexity.empty()

    val polygons2D = Array(count-1) { x -> Array(count-1) { y ->
        val dot = dots2d[x][y]
        Pane(dot.x + dist / 2, dot.y + dist / 2)
    } }
    val polygonMap = mutableListOf<Pair<Pane, List<Pair<Pane, Line>>>>()
    for (x in 0 until count-1)
        for (y in 0 until count-1) {
            val near = mutableListOf<Pair<Pane, Line>>()
            if (x != 0)
                near.add(polygons2D[x-1][y] to yLines[x][y])
            if (x != count - 2)
                near.add(polygons2D[x+1][y] to yLines[x+1][y])
            if (y != 0)
                near.add(polygons2D[x][y-1] to xLines[x][y])
            if (y != count - 2)
                near.add(polygons2D[x][y+1] to xLines[x][y+1])
            polygonMap.add(polygons2D[x][y] to near)
        }

    return Puzzle(
        0,
        mutableListOf(dots2d[0][0]),
        mutableListOf(endDot),
        allDots,
        allLines,
        polygonMap,
        complexity
    )
}
