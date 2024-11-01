import androidx.compose.runtime.*

class Context {
    var editTag by mutableStateOf<Tag?>(null)
    var editFile by mutableStateOf<FileItem?>(null)
    val allTags = mutableStateListOf<Tag>()
    var tagDialogType by mutableStateOf(TagDialogType.Null)
    var fileDialogType by mutableStateOf(FileDialogType.Null)
    val allFiles = mutableStateListOf<FileItem>()

    suspend fun getAllFiles() {
        allFiles.clear()
        allFiles.addAll(queryAllFileItems())
    }
}

val context = Context()