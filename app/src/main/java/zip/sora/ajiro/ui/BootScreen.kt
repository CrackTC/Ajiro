package zip.sora.ajiro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import zip.sora.ajiro.privileged.UserService
import zip.sora.ajiro.privileged.initUserService
import zip.sora.ajiro.utils.LocalShizukuContext

object BootScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val shizukuContext = LocalShizukuContext.current
        val context = LocalContext.current
        var initFailed by rememberSaveable { mutableStateOf<Boolean?>(null) }

        var coroutineScope = rememberCoroutineScope { Dispatchers.IO }
        if (shizukuContext.shizukuBound && initFailed == null) {
            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    if (context.initUserService(UserService.instance!!)) {
                        initFailed = false
                        navigator.replace(MainScreen)
                    } else
                        initFailed = true
                }
            }
        }
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                shizukuContext.run {
                    when {
                        !shizukuRunning -> {}
                        shizukuGranted == null -> LinearProgressIndicator()
                        !shizukuGranted -> {}
                        !shizukuBound -> LinearProgressIndicator()
                        initFailed == null -> LinearProgressIndicator()
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    Text(
                        text = when {
                            !shizukuRunning -> "Shizuku 未激活"
                            shizukuGranted == null -> "获取 Shizuku 权限中"
                            !shizukuGranted -> "Shizuku 授权失败"
                            !shizukuBound -> "创建特权服务中"
                            initFailed == null -> "初始化服务中"
                            initFailed == true -> "服务初始化失败，请确认是否正常进入过一次剧场"
                            else -> "准备就绪"
                        }
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    if (shizukuGranted == false) {
                        Button(onClick = requestPermission) {
                            Text("重试")
                        }
                    }

                    if (initFailed == true) {
                        Button(onClick = { initFailed = null }) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}