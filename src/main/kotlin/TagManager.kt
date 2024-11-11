import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.nio.file.Paths
import java.util.*
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView
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
    val selectedTag = remember { mutableStateListOf<Int>() }
    val selectTypes = remember { mutableStateListOf<FileType>() }
    var noTag by remember { mutableStateOf(false) }
    var allTag by remember { mutableStateOf(false) }
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp).onExternalDrag { externalDragValue ->
            val dragData = externalDragValue.dragData
            if (dragData is DragData.FilesList) {
                dragData.readFiles().map {
                    val isWin = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")
                    val pathString = if (isWin) {
                        it.replace("file:/", "")
                    } else {
                        it.replace("file:", "")
                    }
                    val fileName = Paths.get(pathString).fileName
                    FileItem(fileName.name, mutableStateListOf(), pathString)
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
//                        item {
//                            ListItem(
//                                modifier = Modifier.clickable {
//                                    allTag = !allTag
//                                    if (allTag) {
//                                        noTag = false
//                                    }
//                                },
//                                text = { Text("All") },
//                                trailing = {
//                                    if (allTag) {
//                                        Icon(Icons.Default.Star, contentDescription = "Selected")
//                                    }
//                                }
//                            )
//                        }
                        item {
                            ListItem(
                                modifier = Modifier.clickable {
                                    noTag = !noTag
                                    if (noTag) {
                                        allTag = false
                                    }
                                },
                                text = { Text("No Tag") },
                                trailing = {
                                    if (noTag) {
                                        Icon(Icons.Default.Star, contentDescription = "Selected")
                                    }
                                }
                            )
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
                                CheckListItem(tag.name) {
                                    if (it) {
                                        selectedTag.add(tag.id!!)
                                    } else {
                                        selectedTag.remove(tag.id!!)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right side: File list
                    LazyColumn(modifier = Modifier.weight(2f).fillMaxHeight()) {
                        item {
                            Column {
                                // 全选复选框
                                CheckboxWithLabel("Select All", context.isAllSelected) { checked ->
                                    context.isAllSelected = checked
                                    context.updateSelection(checked)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Files", style = MaterialTheme.typography.h6)
                                    Spacer(modifier = Modifier.weight(1f))
                                    FileType.values().forEach {
                                        val isChecked = selectTypes.contains(it)
                                        CheckboxWithLabel(it.name, isChecked) { checked ->
                                            if (checked) {
                                                selectTypes.add(it)
                                            } else {
                                                selectTypes.remove(it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        items(allFiles.filter { file ->
                            (if (noTag) {
                                file.tags.isEmpty()
                            } else if (allTag) {
                                true
                            } else {
                                selectedTag.isEmpty() || file.tags.any { selectedTag.contains(it.id) }
                            }) &&
                                    (searchQuery.isBlank() || file.name.contains(searchQuery, ignoreCase = true)) &&
                                    (selectTypes.isEmpty() || selectTypes.contains(file.fileType))
                        }) { file ->
                            ContextMenuArea(items = {
                                listOf(
                                    ContextMenuItem("Remove") {
                                        context.editFile = file
                                        context.fileDialogType = FileDialogType.Remove
                                    },
                                    ContextMenuItem("Open In Folder") {
                                        openFileDirectory(file.path)
                                    },
                                    ContextMenuItem("Open") {
                                        openFile(file.path)
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
                                                        removeFileTag(file.id!!, tag.id!!)
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
                                                    insertFileTag(file.id!!, it.id!!)
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