import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        visible = true,
        onCloseRequest = { exitApplication() },
        state = WindowState(
            size = DpSize(1280.dp, 800.dp),
            position = WindowPosition(alignment = Alignment.Center)
        ),
        title = "文件标签管理工具"
    ) {
        FileTagManagerApp()
    }
}