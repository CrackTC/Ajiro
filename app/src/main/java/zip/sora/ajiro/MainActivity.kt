package zip.sora.ajiro

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import zip.sora.ajiro.privileged.IUserService
import zip.sora.ajiro.privileged.UserService
import zip.sora.ajiro.privileged.initUserService
import zip.sora.ajiro.ui.BootScreen
import zip.sora.ajiro.ui.theme.AjiroTheme
import zip.sora.ajiro.utils.DefaultNavigatorScreenTransition
import zip.sora.ajiro.utils.LocalShizukuContext
import zip.sora.ajiro.utils.ShizukuContext
import zip.sora.ajiro.utils.requestShizukuPermission

private const val SHIZUKU_REQUEST_CODE = 765

class MainActivity : ComponentActivity() {

    private val userServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(
                BuildConfig.APPLICATION_ID,
                UserService::class.java.name
            )
        )
            .daemon(false)
            .processNameSuffix("privileged")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

    private lateinit var onBinderReceivedListener: Shizuku.OnBinderReceivedListener
    private lateinit var onBinderDeadListener: Shizuku.OnBinderDeadListener

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepOnSplashScreen = true
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { keepOnSplashScreen }
        enableEdgeToEdge()
        setContent {
            AjiroTheme {
                var shizukuRunning by rememberSaveable { mutableStateOf(false) }
                var shizukuGranted by rememberSaveable { mutableStateOf<Boolean?>(null) }
                var shizukuBound by rememberSaveable { mutableStateOf(false) }
                val serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        if (service != null && service.pingBinder()) {
                            UserService.instance = IUserService.Stub.asInterface(service)
                        }
                        shizukuBound = true
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        shizukuBound = false
                        UserService.instance = null
                    }
                }

                onBinderReceivedListener = {
                    shizukuRunning = true
                    if (UserService.instance == null) {
                        lifecycleScope.launch {
                            if (requestShizukuPermission(SHIZUKU_REQUEST_CODE).also {
                                    shizukuGranted = it
                                }) {
                                Shizuku.bindUserService(userServiceArgs, serviceConnection)
                            }
                        }
                    }
                }

                onBinderDeadListener = { shizukuRunning = false }

                LaunchedEffect(Unit) {
                    Shizuku.addBinderReceivedListenerSticky(onBinderReceivedListener)
                    Shizuku.addBinderDeadListener(onBinderDeadListener)
                    keepOnSplashScreen = false
                }

                CompositionLocalProvider(
                    LocalShizukuContext provides ShizukuContext(
                        shizukuRunning = shizukuRunning,
                        shizukuGranted = shizukuGranted,
                        shizukuBound = shizukuBound,
                        requestPermission = onBinderReceivedListener::onBinderReceived
                    )
                ) {
                    Navigator(
                        screen = BootScreen,
                        disposeBehavior = NavigatorDisposeBehavior(
                            disposeNestedNavigators = false,
                            disposeSteps = true
                        )
                    ) { navigator ->
                        DefaultNavigatorScreenTransition(navigator)
                        val shizukuContext = LocalShizukuContext.current
                        LaunchedEffect(navigator.lastItem, shizukuContext) {
                            if (navigator.lastItem == BootScreen) {
                                return@LaunchedEffect
                            }
                            if (!shizukuContext.shizukuRunning ||
                                shizukuContext.shizukuGranted != true ||
                                !shizukuContext.shizukuBound
                            ) {
                                navigator.popUntilRoot()
                                navigator.replace(BootScreen)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(onBinderReceivedListener)
        Shizuku.removeBinderDeadListener(onBinderDeadListener)
    }
}