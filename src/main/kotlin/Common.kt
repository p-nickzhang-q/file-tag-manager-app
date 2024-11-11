@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import cn.hutool.core.exceptions.ValidateException
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import javax.swing.JOptionPane

suspend fun MutableList<FileItem>.ifNotExistThenAdd(fileItem: FileItem) {
    if (this.none { it.path == fileItem.path }) {
        this.add(fileItem)
        insertFileItem(fileItem)
    }
}

@Composable
fun FloatingDropdownButton(
    items: List<String>,
    onItemSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FloatingActionButton(
            onClick = { expanded = true }) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(onClick = {
                    onItemSelected(item)
                    expanded = false
                }) {
                    Text(text = item)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SelectAddTag(
    items: List<Tag>,
    onItemSelected: (Tag) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Chip(
            onClick = {
                expanded = true
            },
            leadingIcon = {
                Icon(Icons.Default.Add, contentDescription = "Add tag")
            },
        ) {
            Text("Add Tag")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(onClick = {
                    onItemSelected(item)
                    expanded = false
                }) {
                    Text(text = item.name)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TagDialog() {
    val rememberCoroutineScope = rememberCoroutineScope()
    fun onClose() {
        context.tagDialogType = TagDialogType.Null
    }
    when (context.tagDialogType) {
        TagDialogType.Edit -> {
            var newTagName by remember { mutableStateOf(context.editTag?.name ?: "") }
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Edit Tag") },
                text = {
                    TextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("New Tag Name") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        rememberCoroutineScope.launch {
                            context.editTag?.name = newTagName
                            updateTag(context.editTag!!)
                        }
                        onClose()
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Button(onClick = { onClose() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        TagDialogType.Add -> {
            var newTagName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Add Tag") },
                text = {
                    TextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("Tag Name") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        rememberCoroutineScope.launch {
                            val tag = Tag(newTagName)
                            insertTag(tag)
                            context.allTags.add(tag)
                            onClose()
                        }
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    Button(onClick = { onClose() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        TagDialogType.Remove -> {
            AlertDialog(
                onDismissRequest = { onClose() },
                title = { Text("Confirm Delete") },
                text = { Text("Are you sure you want to delete this tag?") },
                confirmButton = {
                    Button(onClick = {
                        rememberCoroutineScope.launch {
                            deleteTag(context.editTag?.id!!)
                            context.allTags.remove(context.editTag)
                            context.getAllFiles()
                        }
                        onClose()
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(onClick = { onClose() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        TagDialogType.Null -> {}
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FileDialog() {
    val rememberCoroutineScope = rememberCoroutineScope()
    fun close() {
        context.fileDialogType = FileDialogType.Null
    }

    when (context.fileDialogType) {
        FileDialogType.Remove -> {
            val selectFiles = context.selectFiles()
            val message = if (selectFiles.isNotEmpty()) {
                "Are you sure you want to delete the selected tag file?"
            } else {
                "Are you sure you want to delete this tag file?"
            }
            AlertDialog(
                onDismissRequest = { close() },
                title = { Text("Confirm Delete") },
                text = { Text(message) },
                confirmButton = {
                    Button(onClick = {
                        rememberCoroutineScope.launch {
                            if (selectFiles.isNotEmpty()) {
                                selectFiles.forEach {
                                    removeFile(it)
                                }
                            } else {
                                removeFile(context.editFile!!)
                            }
                        }
                        close()
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(onClick = { close() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        else -> {

        }
    }
}

private suspend fun removeFile(fileItem: FileItem) {
    context.allFiles.remove(fileItem)
    removeFileItem(fileItem.id!!)
}

enum class FileDialogType {
    Remove, Null
}

enum class TagDialogType {
    Edit, Add, Remove, Null
}

fun fileKey(path: String): String {
    val attributes = Files.readAttributes(Paths.get(path), BasicFileAttributes::class.java)
    return attributes.fileKey().toString()
}

fun openFile(filePath: String) {
    catchError {
        val file = File(filePath)
        if (file.exists()) {
            Desktop.getDesktop().open(file)
        } else {
            throw ValidateException("File does not exist.")
        }
    }
}

fun openFileDirectory(filePath: String) {
    catchError {
        val file = File(filePath)
        if (file.exists()) {
            Desktop.getDesktop().open(file.parentFile)
        } else {
            throw ValidateException("File does not exist.")
        }
    }
}

fun catchError(func: () -> Unit) {
    try {
        func()
    } catch (e: Exception) {
        JOptionPane.showMessageDialog(
            java.awt.Window.getWindows().firstOrNull(),
            e.message,
            "error",
            JOptionPane.ERROR_MESSAGE
        )
    }
}


fun String.decode(): String {
    return URLDecoder.decode(this, StandardCharsets.UTF_8)
}

enum class FileType(val extensions: List<String>) {
    VIDEO(listOf("mp4", "mkv", "avi", "mov", "flv")),
    AUDIO(
        listOf(
            "mp3",
            "wav",
            "flac",
            "aac",
            "ogg"
        )
    ),
    DOCUMENT(listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx")),
    IMAGE(
        listOf(
            "jpg",
            "jpeg",
            "png",
            "gif",
            "bmp",
            "tiff"
        )
    ),
    UNKNOWN(listOf()),
    FOLDER(listOf());

    companion object {
        fun getFileType(path: String): FileType {
            val file = File(path)
            if (file.isDirectory) {
                return FOLDER
            }
            return when (file.extension.lowercase()) {
                in VIDEO.extensions -> VIDEO
                in AUDIO.extensions -> AUDIO
                in DOCUMENT.extensions -> DOCUMENT
                in IMAGE.extensions -> IMAGE
                else -> UNKNOWN
            }
        }
    }
}

@Composable
fun CheckboxWithLabel(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
        Text(text = label) // 显示 CheckBox 的标签
    }
}

fun main() {
    val file = File("/home/dev/ComposeForDesktopDemo/公共的")
    if (file.exists()) {
        println(
            file.isDirectory
        )
    }
}