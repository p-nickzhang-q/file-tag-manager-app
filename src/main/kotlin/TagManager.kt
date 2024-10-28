import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Component
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.io.File
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileSystemView

data class FileItem(val name: String, val tags: List<String>, val path: String)

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun FileTagManagerApp() {
    // Sample data for demonstration
    val allTags = remember { mutableStateListOf("Work", "Personal", "Important", "Study") }
    val allFiles = remember {
        mutableStateListOf(
            FileItem("Document1.pdf", listOf("Work", "Important"), "/path/to/Document1.pdf"),
            FileItem("Photo.png", listOf("Personal"), "/path/to/Photo.png"),
            FileItem("Assignment.docx", listOf("Study"), "/path/to/Assignment.docx"),
            FileItem("Notes.txt", listOf("Work", "Study"), "/path/to/Notes.txt")
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }

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
                            FileItem(it.name, listOf(), it.absolutePath)
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
                            ListItem(
                                modifier = Modifier.clickable {
                                    selectedTag = if (selectedTag == tag) null else tag
                                },
                                text = { Text(tag) },
                                trailing = {
                                    if (selectedTag == tag) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected")
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right side: File list
                    LazyColumn(modifier = Modifier.weight(2f).fillMaxHeight()) {
                        item {
                            Text("Files", style = MaterialTheme.typography.h6)
                        }
                        items(allFiles.filter { file ->
                            (selectedTag == null || file.tags.contains(selectedTag)) &&
                                    (searchQuery.isBlank() || file.name.contains(searchQuery, ignoreCase = true))
                        }) { file ->
                            ListItem(
                                text = { Text(file.name) },
                                secondaryText = { Text("Tags: ${file.tags.joinToString(", ")}") }
                            )
                        }
                    }
                }
            }

            // Add button in the bottom right corner
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomStart
            ) {
                FloatingActionButton(
                    onClick = { showMenu = !showMenu }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }

                // Dropdown menu for adding file/folder
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(onClick = {
                        showMenu = false
                        selectFiles(allFiles)
                        // Handle "Add File" action here
                    }) {
                        Text("Add File")
                    }
                    DropdownMenuItem(onClick = {
                        showMenu = false
                        selectFolder(allFiles)
                        // Handle "Add Folder" action here
                    }) {
                        Text("Add Folder")
                    }
                }
            }

        }
    }
}

fun selectFiles(allFiles: MutableList<FileItem>) {
    val fileDialog = FileDialog(null as Frame?, "Select Files", FileDialog.LOAD).apply {
        isMultipleMode = true
        isVisible = true
    }
    fileDialog.files?.forEach { file ->
        allFiles.ifNotExistThenAdd(FileItem(file.name, listOf(), file.path))
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
        allFiles.ifNotExistThenAdd(FileItem(folder.name, listOf(), folder.absolutePath))
    }
}