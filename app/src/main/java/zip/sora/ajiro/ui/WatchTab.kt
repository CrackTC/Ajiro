package zip.sora.ajiro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import zip.sora.ajiro.R
import zip.sora.ajiro.privileged.IWatchListener
import zip.sora.ajiro.privileged.UserService
import zip.sora.ajiro.utils.drawVerticalScrollbar

object WatchTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = painterResource(R.drawable.ic_code)
            return remember {
                TabOptions(1u, "监听", icon)
            }
        }

    enum class WatchState {
        Stopped,
        Starting,
        Watching
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val fileNameList = rememberSaveable { mutableStateListOf<String>() }
        var state by rememberSaveable { mutableStateOf(WatchState.Stopped) }
        val coroutineScope = rememberCoroutineScope { Dispatchers.Default }
        val listState = rememberLazyListState()
        val isAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

        var filterText by rememberSaveable { mutableStateOf(true) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("监听") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        if (isAtTop) Color.Unspecified
                        else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    actions = {
                        IconButton(onClick = { filterText = !filterText }) {
                            if (!filterText) {
                                Icon(painter = painterResource(R.drawable.ic_text), null)
                            } else {
                                Icon(painter = painterResource(R.drawable.ic_cube), null)
                            }
                        }
                        IconButton(
                            onClick = { fileNameList.clear() },
                            enabled = fileNameList.isNotEmpty(),
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_delete), null)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        when (state) {
                            WatchState.Stopped -> {
                                state = WatchState.Starting
                                coroutineScope.launch {
                                    UserService.instance?.startWatchFiles(
                                        object : IWatchListener.Stub() {
                                            override fun onOpenFile(name: String?) {
                                                if (name != null && fileNameList.lastOrNull() != name) {
                                                    fileNameList.add(name)
                                                }
                                            }
                                        })?.also { state = WatchState.Watching }
                                }
                            }

                            WatchState.Watching -> {
                                UserService.instance?.stopWatchFiles()
                                    ?.also { state = WatchState.Stopped }
                            }

                            WatchState.Starting -> {}
                        }
                    }) {

                    when (state) {
                        WatchState.Stopped -> {
                            Icon(painter = painterResource(R.drawable.ic_play), null)
                        }

                        WatchState.Watching -> {
                            Icon(painter = painterResource(R.drawable.ic_pause), null)
                        }

                        WatchState.Starting -> {
                            CircularProgressIndicator(modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }) { innerPadding ->
            if (fileNameList.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .drawVerticalScrollbar(listState),
                    state = listState
                ) {
                    items(fileNameList) { fileName ->
                        if (!filterText || fileName.endsWith(".gtx.unity3d"))
                            ListItem(
                                modifier = Modifier.clickable {
                                    if (state == WatchState.Watching) {
                                        UserService.instance?.stopWatchFiles()
                                            ?.also { state = WatchState.Stopped }
                                    }
                                    navigator.push(EditScreen(fileName))
                                },
                                headlineContent = {
                                    Text(fileName)
                                }
                            )
                    }
                }
                var previousSize by remember { mutableIntStateOf(fileNameList.size) }
                LaunchedEffect(fileNameList.size) {
                    if (fileNameList.size > previousSize) {
                        val lastItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                        val isAtBottom = lastItem == null ||
                                lastItem.index >= previousSize - listState.layoutInfo.visibleItemsInfo.size

                        if (isAtBottom) {
                            listState.animateScrollToItem(
                                fileNameList.size - 1
                            )
                        }
                    }
                    previousSize = fileNameList.size
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Text("什么都没有...")
                }
            }
        }
    }
}