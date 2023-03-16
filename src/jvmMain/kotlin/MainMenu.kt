import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import json.copyPuzzle
import json.loadPuzzles
import json.savePath
import json.savePuzzles
import kotlinx.coroutines.*
import java.awt.Desktop
import java.net.URI
import java.util.*

private val puzzleItemDp = 200.dp
private val puzzleDrawDp = 400.dp
private val padding = 5.dp

private const val puzzlesInRow = 3

private val windowWidth = (puzzleItemDp + padding * 2) * puzzlesInRow
private const val windowRatio = 1f // 16/9f

@OptIn(ExperimentalMaterialApi::class)
fun main() {
    application {
        val puzzles = remember { mutableStateOf<MutableList<Puzzle>?>(null) }

        val loadingViewModel = remember { LoadingViewModel() }
        LaunchedEffect(Unit) {
            loadingViewModel.load {
                try {
                    puzzles.value = loadPuzzles()
                } catch (e: Exception) {
                    loadingViewModel.loadingError = true to e.stackTraceToString()
                }
            }
        }

        val selectedPuzzleIndex = remember { mutableStateOf(-1) }
        val selectedPuzzle = remember { mutableStateOf<Puzzle?>(null) }

        val isInMenu = remember { mutableStateOf(true) } // if false make menu invisible and open editor

        val isUpdateMenu = remember { mutableStateOf(false) } // if true disable draw puzzles
        val save = {
            if (loadingViewModel.loading.not()) {
                puzzles.value!!.sortBy { it.id }
                savePuzzles(puzzles.value!!)

                // its redraw all puzzle
                isUpdateMenu.value = true
                isUpdateMenu.value = false
            }
        }

        val isChangingIndex = remember { mutableStateOf(false) }
        val changeIndexData = remember { mutableStateOf<ChangeIndexData?>(null) }

        if (loadingViewModel.loadingError.first)
            Window(::exitApplication, title = "Loading error", resizable = false) {
                TextField(
                    "Error while loading $savePath\n\n" + loadingViewModel.loadingError.second
                    , {})
            }

        // menu window
        Window(
            onCloseRequest = { save(); exitApplication() },
            state = WindowState(size = DpSize(windowWidth, windowWidth / windowRatio),
                position = WindowPosition(Alignment.Center)),
            visible = isInMenu.value && loadingViewModel.loadingError.first.not(),
            resizable = false,
            title = "Puzzle Editor",
        ) {
            val topMenuBox = @Composable { onClick: () -> Unit, content: @Composable BoxScope.() -> Unit ->
                Surface(
                    onClick = onClick,
                    modifier = Modifier.size(40.dp - 4.dp).padding(2.dp),
                    shape = RoundedCornerShape(10),
                    color = Color.Gray
                ) {
                    Box(Modifier, Alignment.Center, content = content)
                }
            }
            Column {
                if (loadingViewModel.loading) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("Loading...")
                    }
                } else
                    LazyColumn {
                        items(
                            if (isUpdateMenu.value.not())
                                (puzzles.value!!.size + puzzlesInRow - 1) / puzzlesInRow
                            else 0
                        ) { rowInd ->
                            Row(Modifier.fillMaxWidth()) {
                                for (colInd in 0 until puzzlesInRow) {
                                    val index = rowInd * puzzlesInRow + colInd
                                    if (puzzles.value!!.size > index) // isUpdateMenu is only for updating list
                                        drawPuzzle(puzzles.value!![index], true, {
                                            // click on index
                                            changeIndexData.value =
                                                ChangeIndexData(index, puzzles.value!![index].id)
                                            isChangingIndex.value = true
                                        }) {
                                            selectedPuzzleIndex.value = index
                                            selectedPuzzle.value = puzzles.value!![index].copyPuzzle()

                                            isInMenu.value = false
                                        }
                                }
                            }
                        }
                        item {
                            Box(Modifier.fillMaxWidth().height(windowWidth / windowRatio / 2))
                        }
                    }
            }
            Row {
                topMenuBox({
                    if (loadingViewModel.loading.not()) {
                        puzzles.value!!.add(createSimpleRectPuzzle().also {
                            if (puzzles.value!!.size != 0)
                                it.id = puzzles.value!!.last().id + 1
                            else
                                it.id = 0
                        })
                        save()
                    }
                }) { Text("+") }

                topMenuBox({
                    openInBrowser(URI("https://github.com/Odrian/The-Witness-Puzzle-Editor"))
                }) {
                    Text("?")
                }
            }

            if (isChangingIndex.value) {
                val id = remember { mutableStateOf(changeIndexData.value!!.id.toString()) }
                AlertDialog(
                    onDismissRequest = { isChangingIndex.value = false },
                    shape = RoundedCornerShape(10),
                    buttons = {
                        val errorText = remember { mutableStateOf("") }
                        Column(Modifier.padding(10.dp)) {
                            Text("Change id. It was \'${changeIndexData.value?.id}\'", Modifier.padding(5.dp))
                            TextField(id.value, { id.value = it }, label = { Text("new id") })
                            Text(errorText.value, color = Color.Red)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Button({
                                    changeIndexData.value = null
                                    isChangingIndex.value = false
                                    save()
                                }) { Text("cancel") }
                                Button({
                                    puzzles.value!!.removeAt(changeIndexData.value!!.index)

                                    changeIndexData.value = null
                                    isChangingIndex.value = false
                                    save()
                                }) { Text("delete") }
                                Button({
                                    val newId = id.value.toIntOrNull()
                                    val index = changeIndexData.value!!.index
                                    if (newId == null) {
                                        errorText.value = "Can't cast id to int"
                                    } else {
                                        var isUnique = puzzles.value!!.firstOrNull { it.id == newId } == null
                                        isUnique = isUnique || (newId == changeIndexData.value!!.id)

                                        if (isUnique.not()) {
                                            errorText.value = "Not unique id. You can use \'${puzzles.value!!.last().id + 1}\'"
                                        } else {
                                            puzzles.value!![index].id = newId

                                            isChangingIndex.value = false
                                            changeIndexData.value = null
                                            save()
                                        }
                                    }
                                }) {
                                    Text("save")
                                }

                            }
                        }
                    },
                )
            }
        }
        if (isInMenu.value.not())
            editor(selectedPuzzle.value!!) {
                if (it) {
                    puzzles.value!![selectedPuzzleIndex.value] = selectedPuzzle.value!!
                    save()
                }
                selectedPuzzle.value = null

                isInMenu.value = true
            }
    }
}

private class LoadingViewModel {
    var loading by mutableStateOf(true)
    var loadingError by mutableStateOf(false to "")

    @OptIn(DelicateCoroutinesApi::class)
    fun load(block: CoroutineScope.() -> Unit) {
        GlobalScope.launch {
            delay(300)
            block()
            loading = false
        }
    }
}

@Suppress("SameParameterValue")
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun drawPuzzle(puzzle: Puzzle, showIndex: Boolean, onIndexClick: () -> Unit = {}, onClick: () -> Unit) {
    Box(
        Modifier
            .size(puzzleItemDp + padding * 2)
            .padding(padding)
            .background(Color.LightGray, RoundedCornerShape(10)),
        Alignment.TopCenter
    ) {
        // this offset works well, and I don't touch it
        // (+-2 because all drawing from the left top corner and drawPuzzle flip image)
        drawPuzzle(puzzleItemDp, puzzleDrawDp, puzzle, backgroundColor = Color.LightGray)
        Surface(
            onClick = onClick,
            Modifier.size(puzzleItemDp),
            shape = RoundedCornerShape(10),
            color = Color.Transparent
        ) {}
        if (showIndex)
            Surface(
                onIndexClick,
                Modifier.padding(padding).size(20.dp),
                shape = RoundedCornerShape(30),
                color = Color(.4f, .4f, 1f),
            ) {
                Box(Modifier, Alignment.Center) { Text("${puzzle.id}") }
            }
    }
}

private data class ChangeIndexData(val index: Int, val id: Int)

fun openInBrowser(uri: URI) {
    val osName by lazy(LazyThreadSafetyMode.NONE) { System.getProperty("os.name").lowercase(Locale.getDefault()) }
    val desktop = Desktop.getDesktop()
    when {
        Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE) -> desktop.browse(uri)
        "mac" in osName -> Runtime.getRuntime().exec("open $uri")
        "nix" in osName || "nux" in osName -> Runtime.getRuntime().exec("xdg-open $uri")
        else -> {
//            throw RuntimeException("cannot open $uri")
        }
    }
}
