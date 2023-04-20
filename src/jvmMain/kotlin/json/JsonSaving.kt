package json

import puzzle.Dot
import puzzle.Pane
import puzzle.Puzzle
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

const val savePath = "puzzles.json"

fun savePuzzles(puzzles: List<Puzzle>) {
    val puzzlesJson = puzzles.map { it.toPuzzleJson() }
    val stringData = Json.encodeToString(puzzlesJson)

    // save stringData to file
    File(savePath).writeText(stringData)
}

fun loadPuzzles(): MutableList<Puzzle> {
    // get stringData from file
    val file = File(savePath)
    if (file.canRead().not())
        return mutableListOf()
    val stringData = file.readText()
    val puzzlesJson = Json { ignoreUnknownKeys = true }.decodeFromString<List<PuzzleJson>>(stringData)
    return puzzlesJson.map {
        it.also { it.complexityJson.removeNulls() }.toPuzzle()
    }.toMutableList()
}

// it seems to be the simplest way to copy the puzzle
fun Puzzle.copyPuzzle(): Puzzle {
    val puzzleJson = this.toPuzzleJson()
    val newPuzzleJson = with(puzzleJson) {
        PuzzleJson(
            name,
            startDots,
            endDots,
            dots.map { Dot(it.x, it.y) }.toMutableList(),
            lines,
            panes.map { Pane(it.x, it.y) }.toMutableList(),
            paneMap,
            complexityJson
        )
    }
    return newPuzzleJson.toPuzzle()
}
