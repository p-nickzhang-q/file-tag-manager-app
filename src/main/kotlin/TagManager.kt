import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.awt.FileDialog
import java.awt.Frame
import java.awt.dnd.*
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
@Preview
fun FileTagManagerApp() {

    // Sample data for demonstration
    val allTags =
        remember {
            mutableStateListOf<Tag>()
        }
    val allFiles = remember {
        mutableStateListOf<FileItem>()
    }

    var dialogType by remember { mutableStateOf(DialogType.Null) }

    LaunchedEffect(Unit) {
        connection()?.use {
            it.ifNotExistCreateTable()
            allTags.addAll(it.getAllTags())
            allFiles.addAll(it.queryAllFileItems())
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        // Setup DropTarget for detecting drag-and-drop events
        val target = DropTarget().apply {
            addDropTargetListener(object : DropTargetListener {
                override fun dragEnter(dtde: DropTargetDragEvent?) {
                    println("dragEnter")
                }

                override fun dragOver(dtde: DropTargetDragEvent?) {
                    println("dragOver")
                }

                override fun dropActionChanged(dtde: DropTargetDragEvent?) {
                    println("dropActionChanged")
                }

                override fun dragExit(dte: DropTargetEvent?) {
                    println("dragExit")
                }

                override fun drop(event: DropTargetDropEvent?) {
                    println("drop")
                    if (event == null) {
                        return
                    }
                    event.acceptDrop(DnDConstants.ACTION_COPY)
                    val transfer = event.transferable
                    if (transfer.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                        val fileList =
                            transfer.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor) as List<File>
                        fileList.map {
                            FileItem(it.name, mutableStateListOf(), it.absolutePath)
                        }.forEach {
                            allFiles.ifNotExistThenAdd(it)
                        }
                        event.dropComplete(true)
                    } else {
                        event.dropComplete(false)
                    }
                }

            })
        }
        // Setting the DropTarget to the window
        java.awt.Window.getWindows().firstOrNull()?.dropTarget = target
    }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                            Text("Tags", style = MaterialTheme.typography.h6)
                        }
                        items(allTags) { tag ->
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                ListItem(
                                    modifier = Modifier.clickable {
                                        selectedTag = if (selectedTag == tag.id) null else tag.id
                                    }.onClick(
                                        matcher = PointerMatcher.mouse(PointerButton.Secondary), // add onClick for every required PointerButton
                                        keyboardModifiers = { true }, // e.g { isCtrlPressed }; Remove it to ignore keyboardModifiers
                                        onClick = {
                                            expanded = true
                                        }
                                    ),
                                    text = { Text(tag.name) },
                                    trailing = {
                                        if (selectedTag == tag.id) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected")
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(onClick = {
                                        dialogType = DialogType.Edit
                                    }) {
                                        Text("Edit")
                                    }
                                    DropdownMenuItem(onClick = {
                                        dialogType = DialogType.Add
                                    }) {
                                        Text("Add")
                                    }
                                    DropdownMenuItem(onClick = {
                                        dialogType = DialogType.Remove
                                    }) {
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right side: File list
                    LazyColumn(modifier = Modifier.weight(2f).fillMaxHeight()) {
                        item {
                            Text("Files", style = MaterialTheme.typography.h6)
                        }
                        items(allFiles.filter { file ->
                            (selectedTag == null || file.tags.any { it.id == selectedTag }) &&
                                    (searchQuery.isBlank() || file.name.contains(searchQuery, ignoreCase = true))
                        }) { file ->
                            Column {
                                Text(file.name, style = MaterialTheme.typography.subtitle1)
                                Text(file.path, style = MaterialTheme.typography.subtitle2)
                                Row {
                                    file.tags.forEach { tag ->
                                        Chip(
                                            onClick = {
                                                file.tags.remove(tag)
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
                                        if (!file.tags.contains(it)) {
                                            file.tags.add(it)
                                        }
                                    }
                                }
                            }
                            Divider()
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
                    listOf("Add File", "Add Folder")
                ) {
                    when (it) {
                        "Add File" -> {
                            selectFiles(allFiles)
                        }

                        "Add Folder" -> {
                            selectFolder(allFiles)
                        }

                        else -> {

                        }
                    }
                }
            }
        }

        Dialog(dialogType, onClose = { dialogType = DialogType.Null }, onConfirm = {
            println(it)
        })
    }
}

fun selectFiles(allFiles: MutableList<FileItem>) {
    val fileDialog = FileDialog(null as Frame?, "Select Files", FileDialog.LOAD).apply {
        isMultipleMode = true
        isVisible = true
    }
    fileDialog.files?.forEach { file ->
        allFiles.ifNotExistThenAdd(FileItem(file.name, mutableStateListOf(), file.path))
    }
}

fun selectFolder(allFiles: MutableList<FileItem>) {
    val fileChooser = JFileChooser(FileSystemView.getFileSystemView()).apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY // Only select directories
        isAcceptAllFileFilterUsed = false // Disable "All files" option
        dialogTitle = "Select Folder"
    }

    val result = fileChooser.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        val folder = fileChooser.selectedFile
        allFiles.ifNotExistThenAdd(FileItem(folder.name, mutableStateListOf(), folder.absolutePath))
    }
}