package zip.sora.ajiro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import zip.sora.ajiro.R

object PatchTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = painterResource(R.drawable.ic_patch)
            return remember {
                TabOptions(0u, "安装", icon)
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val screenModel = rememberScreenModel { PatchTabModel() }
        val uiState by screenModel.uiState.collectAsState()
        LaunchedEffect(Unit) {
            screenModel.init(context.getExternalFilesDir(null)!!.path)
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("安装") },
                    actions = {
                        IconButton(
                            onClick = {
                                screenModel.restore()
                            },
                            enabled = uiState.status == PatchTabStatus.PENDING
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_history), null)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ListItem(
                    overlineContent = {
                        Text("本地版本")
                    },
                    headlineContent = {
                        Text(
                            uiState.localSha ?: "无",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )

                var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
                ListItem(
                    overlineContent = {
                        Text("最新版本")
                    },
                    headlineContent = {
                        Text(
                            when (uiState.remoteSha) {
                                null -> "获取中"
                                "" -> "获取失败"
                                else -> uiState.remoteSha!!
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                ListItem(
                    overlineContent = {
                        Text(
                            when (uiState.status) {
                                PatchTabStatus.PENDING -> "待机"
                                PatchTabStatus.DOWNLOADING -> "下载中"
                                PatchTabStatus.UNPACKING -> "解压中"
                                PatchTabStatus.PATCHING -> "安装中"
                                PatchTabStatus.RESTORING -> "还原中"
                            }
                        )
                    },
                    headlineContent = {
                        if (uiState.status == PatchTabStatus.RESTORING) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                progress = { uiState.progress }
                            )
                        }
                    },
                    supportingContent = {
                        Text(uiState.progressDesc?.invoke(context) ?: "")
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Button(
                        onClick = {
                            screenModel.update(context.getExternalFilesDir(null)!!.path)
                        },
                        enabled = (uiState.remoteSha ?: "") != "" &&
                                uiState.remoteSha != uiState.localSha &&
                                uiState.status == PatchTabStatus.PENDING
                    ) {
                        Text("更新")
                    }

                    Button(
                        onClick = {
                            screenModel.patch(context.getExternalFilesDir(null)!!.path)
                        },
                        enabled = uiState.localSha != null && uiState.status == PatchTabStatus.PENDING
                    ) {
                        Text("这是什么按钮，按一下")
                    }
                }
            }
        }
    }
}