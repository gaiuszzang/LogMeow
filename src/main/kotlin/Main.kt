import adb.AdbService
import adb.data.AdbDevice
import adb.data.AdbDeviceState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import di.appModule
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import ui.MainScreen
import vm.MainViewModel

@Composable
fun App() {
    val viewModel: MainViewModel = koinInject()
    MainScreen(viewModel)
}

fun main() = application {
    KoinApplication(
        application = {
            modules(appModule)
        }
    ) {
        Window(
            onCloseRequest = {
                exitApplication()
            },
            title = "LogMeow",
            state = rememberWindowState(
                size = DpSize(1400.dp, 900.dp)
            )
        ) {
            window.minimumSize = java.awt.Dimension(800, 400)
            App()
        }
    }
}

// A separate preview function that doesn't rely on Koin
@Preview
@Composable
fun AppPreview() {
    // Create a dummy AdbService that returns a fixed list of devices for the preview
    val dummyAdbService = object : AdbService() {
        override fun getDevicesFlow() = flowOf(
            listOf(
                AdbDevice("emulator-5554", AdbDeviceState.DEVICE),
                AdbDevice("RF8M7212X4E", AdbDeviceState.OFFLINE),
                AdbDevice("some-device-id", AdbDeviceState.UNAUTHORIZED)
            )
        )
    }
    // Create the ViewModel with the dummy service
    val previewViewModel = MainViewModel(dummyAdbService)
    // We need to manually clear it, although for a preview it's not critical
    previewViewModel.onCleared()
    
    MainScreen(previewViewModel)
}

