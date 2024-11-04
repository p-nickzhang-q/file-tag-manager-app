import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.awt.dnd.*
import java.io.File
import java.nio.file.Paths
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView
import kotlin.io.path.absolutePathString
import kotlin.io.path.name

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
@Preview
fun FileTagManagerApp() {
    val rememberCoroutineScope = rememberCoroutineScope()
    // Sample data for demonstration

    val allFiles = remember {
        context.allFiles
    }

    val allTags = remember { context.allTags }
    LaunchedEffect(Unit) {
        ifNotExistCreateTable()
        allTags.addAll(getAllTags())
        context.getAllFiles()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<Int?>(null) }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp).onExternalDrag { externalDragValue ->
            val dragData = externalDragValue.dragData
            if (dragData is DragData.FilesList) {
                dragData.readFiles().map {
                    val fileName = Paths.get(it).fileName
                    FileItem(fileName.name, mutableStateListOf(), fileName.absolutePathString())
                }.forEach {
                    rememberCoroutineScope.launch {
                        allFiles.ifNotExistThenAdd(it)
                    }
                }
            }

        }) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search bar at the top
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search files") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxSize()) {
                    // Left side: Tag list
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 8.dp)
                    ) {
                        item {
                            ContextMenuArea(items = {
                                listOf(ContextMenuItem("Add") {
                                    context.tagDialogType = TagDialogType.Add
                                })
                            }) {
                                Text("Tags", style = MaterialTheme.typography.h6)
                            }
                        }
                        items(allTags) { tag ->
                            ContextMenuArea(items = {
                                listOf(
                                    ContextMenuItem("Edit") {
                                        context.editTag = tag
                                        context.tagDialogType = TagDialogType.Edit
                                    },
                                    ContextMenuItem("Add") {
                                        context.tagDialogType = TagDialogType.Add
                                    },
                                    ContextMenuItem("Remove") {
                                        context.editTag = tag
                                        context.tagDialogType = TagDialogType.Remove
                                    }
                                )
                            }) {
                                ListItem(
                                    modifier = Modifier.clickable {
                                        selectedTag = if (selectedTag == tag.id) null else tag.id
                                    },
                                    text = { Text(tag.name) },
                                    trailing = {
                                        if (selectedTag == tag.id) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right side: File list
                    LazyColumn(modifier = Modifier.weight(2f).fillMaxHeight()) {
                        item {
                            Column {
                                // 全选复选框
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = context.isAllSelected,
                                        onCheckedChange = { checked ->
                                            context.isAllSelected = checked
                                            context.updateSelection(checked)
                                        }
                                    )
                                    Text("Select All")
                                }
                                Text("Files", style = MaterialTheme.typography.h6)
                            }
                        }
                        items(allFiles.filter { file ->
                            (selectedTag == null || file.tags.any { it.id == selectedTag }) &&
                                    (searchQuery.isBlank() || file.name.contains(searchQuery, ignoreCase = true))
                        }) { file ->
                            ContextMenuArea(items = {
                                listOf(
                                    ContextMenuItem("Remove") {
                                        context.editFile = file
                                        context.fileDialogType = FileDialogType.Remove
                                    }
                                )
                            }) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = file.selected,
                                            onCheckedChange = { isChecked ->
                                                file.selected = isChecked
                                                if (isChecked) {
                                                    context.isAllSelected = allFiles.all { it.selected }
                                                } else {
                                                    context.isAllSelected = false
                                                }
                                            }
                                        )
                                        Column {
                                            Text(file.name, style = MaterialTheme.typography.subtitle1)
                                            Text(file.path, style = MaterialTheme.typography.subtitle2)
                                        }
                                    }
                                    Row {
                                        file.tags.forEach { tag ->
                                            Chip(
                                                onClick = {
                                                    rememberCoroutineScope.launch {
                                                        removeFileTag(file.id, tag.id!!)
                                                        file.tags.remove(tag)
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove tag")
                                                },
                                                colors = ChipDefaults.chipColors(backgroundColor = Color.LightGray)
                                            ) {
                                                Text(tag.name)
                                            }
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        SelectAddTag(allTags) {
                                            rememberCoroutineScope.launch {
                                                if (!file.tags.contains(it)) {
                                                    insertFileTag(file.id, it.id!!)
                                                    file.tags.add(it)
                                                }
                                            }
                                        }
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
            }

            // Add button in the bottom right corner
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingDropdownButton(
                    listOf("Add File")
                ) {
                    when (it) {
                        "Add File" -> {
                            rememberCoroutineScope.launch {
                                selectFiles(allFiles)
                            }
                        }

                        else -> {

                        }
                    }
                }
            }
        }

        TagDialog()
        FileDialog()
    }
}

suspend fun selectFiles(allFiles: MutableList<FileItem>) {
    val fileChooser = JFileChooser(FileSystemView.getFileSystemView()).apply {
        fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES // Only select directories
        isAcceptAllFileFilterUsed = false // Disable "All files" option
        isMultiSelectionEnabled = true
        dialogTitle = "Select Folder"
    }

    val result = fileChooser.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        fileChooser.selectedFiles.forEach {
            allFiles.ifNotExistThenAdd(
                FileItem(
                    it.name,
                    mutableStateListOf(),
                    it.absolutePath
                )
            )
        }
    }
}