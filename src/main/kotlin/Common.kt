@file:OptIn(ExperimentalMaterialApi::class)

import androidx.compose.foundation.layout.Box
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*

fun MutableList<FileItem>.ifNotExistThenAdd(fileItem: FileItem) {
    if (this.none { it.path == fileItem.path }) {
        this.add(fileItem)
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

@Composable
fun Dialog(dialogType: DialogType, onClose: () -> Unit, onConfirm: (String) -> Unit) {
    when (dialogType) {
        DialogType.Edit -> {
            var newTagName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { onClose() },
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
                        onConfirm(newTagName)
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

        DialogType.Add -> {
            var newTagName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { onClose() },
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
                        onConfirm(newTagName)
                        onClose()
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

        DialogType.Remove -> {
            AlertDialog(
                onDismissRequest = { onClose() },
                title = { Text("Confirm Delete") },
                text = { Text("Are you sure you want to delete this tag?") },
                confirmButton = {
                    Button(onClick = {
                        onConfirm("")
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

        DialogType.Null -> {}
    }
}

enum class DialogType {
    Edit, Add, Remove, Null
}