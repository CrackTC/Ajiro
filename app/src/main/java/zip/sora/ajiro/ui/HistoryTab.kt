package zip.sora.ajiro.ui

import android.os.FileUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import zip.sora.ajiro.R
import zip.sora.ajiro.utils.clearHistory
import zip.sora.ajiro.utils.getHistory
import java.io.File

object HistoryTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = painterResource(R.drawable.ic_history)
            return remember {
                TabOptions(2u, "历史", icon)
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val history = remember { mutableStateListOf<String>() }
        LaunchedEffect(Unit) {
            val loadedHistory = context.getHistory()
            history.clear()
            history.addAll(loadedHistory)
        }

        var showClearConfirm by rememberSaveable { mutableStateOf(false) }
        if (showClearConfirm) {
            AlertDialog(
                text = { Text("将会清空未导出的编辑历史") },
                onDismissRequest = { showClearConfirm = false },
                confirmButton = {
                    TextButton(onClick = {
                        val fileDir = context.getExternalFilesDir(null)!!
                        val gtxDir = File(fileDir, "gtx")
                        history.forEach { name ->
                            val file = File(gtxDir, name.removeSuffix(".unity3d"))
                            file.delete()
                        }
                        history.clear()
                        context.clearHistory()
                        showClearConfirm = false
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
                }
            )
        }

        val listState = rememberLazyListState()
        val isAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("历史") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        if (isAtTop) Color.Unspecified
                        else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    actions = {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            enabled = history.isNotEmpty()
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_delete), null)
                        }
                    }
                )
            }
        ) { innerPadding ->
            val navigator = LocalNavigator.currentOrThrow
            if (history.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("什么都没有...")
                }
            }
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                state = listState
            ) {
                items(history) {
                    ListItem(
                        modifier = Modifier.clickable {
                            navigator.push(EditScreen(it))
                        },
                        headlineContent = {
                            Text(it)
                        }
                    )
                }
            }
        }
    }
}