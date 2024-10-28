import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

@Composable
@Preview
fun App(isOpenDialog: MutableState<Boolean>, isOpenWindow: MutableState<Boolean>) {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        showDialog(isOpenDialog, isOpenWindow)
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {
                text = "Hello, Desktop!"
            }) {
                Text(text)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun showDialog(openDialog: MutableState<Boolean>, openWindow: MutableState<Boolean>) {
    if (openDialog.value) {
        AlertDialog(
            modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth(0.5f),
            onDismissRequest = {},
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("alert", style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("close window?", style = MaterialTheme.typography.h6)
                }
            },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Button(
                        onClick = { openDialog.value = false },
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("cancel", style = MaterialTheme.typography.h6)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { openWindow.value = false },
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("confirm", style = MaterialTheme.typography.h6)
                    }
                }
            }
        )
    }
}

fun main() = application {

    val isOpenWindow = remember { mutableStateOf(true) }
    val isOpenDialog = remember { mutableStateOf(false) }
    if (isOpenWindow.value) {
        Window(
            title = "Test",
            onCloseRequest = { isOpenDialog.value = true },
            state = WindowState(size = DpSize(1280.dp, 800.dp), position = WindowPosition(alignment = Alignment.Center))
        ) {
            App(isOpenDialog, isOpenWindow)
        }
    }
}
