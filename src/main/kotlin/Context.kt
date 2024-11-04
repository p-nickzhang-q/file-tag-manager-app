import androidx.compose.runtime.*

class Context {
    var editTag by mutableStateOf<Tag?>(null)
    var editFile by mutableStateOf<FileItem?>(null)
    val allTags = mutableStateListOf<Tag>()
    var tagDialogType by mutableStateOf(TagDialogType.Null)
    var fileDialogType by mutableStateOf(FileDialogType.Null)
    val allFiles = mutableStateListOf<FileItem>()
    var isAllSelected by mutableStateOf(false)

    fun updateSelection(isSelected: Boolean) {
        if (isSelected) {
            allFiles.forEach { it.selected = true }
        } else {
            allFiles.forEach { it.selected = false }
        }
    }

    fun selectFiles() = allFiles.filter { it.selected }

    suspend fun getAllFiles() {
        allFiles.clear()
        allFiles.addAll(queryAllFileItems())
    }
}

val context = Context()