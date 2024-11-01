import androidx.compose.runtime.*

class Context {
    var editTag by mutableStateOf<Tag?>(null)
    val allTags = mutableStateListOf<Tag>()
}

val context = Context()