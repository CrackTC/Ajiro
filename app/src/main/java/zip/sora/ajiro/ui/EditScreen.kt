package zip.sora.ajiro.ui

import android.content.ClipData
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import zip.sora.ajiro.DOWNLOAD_SUBDIR_NAME
import zip.sora.ajiro.R
import zip.sora.ajiro.privileged.UserService
import zip.sora.ajiro.utils.addHistory
import zip.sora.ajiro.utils.copyFileToDownloadFolder
import zip.sora.ajiro.utils.drawVerticalScrollbar
import zip.sora.ajiro.utils.saveGameText

class EditScreen(private val name: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val fileDir = "${context.getExternalFilesDir(null)!!.path}/gtx"
        val screenModel = rememberScreenModel { EditScreenModel(name, fileDir) }
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(Unit) { context.addHistory(name) }

        var hasChange by rememberSaveable { mutableStateOf(false) }
        val listState = rememberLazyListState()
        var filterText by rememberSaveable { mutableStateOf("") }

        var showUnsavedChangeDialog by rememberSaveable { mutableStateOf(false) }
        var showPatchConfirmDialog by rememberSaveable { mutableStateOf(false) }

        val navigator = LocalNavigator.currentOrThrow

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                context.contentResolver.openInputStream(it)?.let { inputStream ->
                    screenModel.import(inputStream)
                    hasChange = true
                }
            }
        }

        if (uiState.dialogMessage != null) {
            AlertDialog(
                text = { Text(uiState.dialogMessage!!) },
                confirmButton = {
                    TextButton(onClick = {
                        screenModel.resolveDialog()
                    }) {
                        Text("确定")
                    }
                },
                onDismissRequest = { screenModel.resolveDialog() }
            )
        }

        if (showUnsavedChangeDialog) {
            AlertDialog(
                text = { Text("有未保存的变更，确定要返回吗") },
                onDismissRequest = {
                    showUnsavedChangeDialog = false
                },
                confirmButton = {
                    TextButton(onClick = {
                        navigator.pop()
                        showUnsavedChangeDialog = false
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showUnsavedChangeDialog = false
                    }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showPatchConfirmDialog) {
            if (uiState.texts.isNotEmpty()) {
                AlertDialog(
                    text = { Text("将要对游戏资源进行修补") },
                    onDismissRequest = { showPatchConfirmDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            screenModel.patch()
                            showPatchConfirmDialog = false
                        }) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showPatchConfirmDialog = false
                        }) {
                            Text("取消")
                        }
                    }
                )
            } else {
                AlertDialog(
                    text = { Text("只支持游戏文本修补") },
                    onDismissRequest = { showPatchConfirmDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showPatchConfirmDialog = false }) {
                            Text("确定")
                        }
                    }
                )
            }
        }

        BackHandler {
            if (hasChange) {
                showUnsavedChangeDialog = true
            } else {
                navigator.pop()
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                var showSearchBar by rememberSaveable { mutableStateOf(false) }
                val focusRequester = remember { FocusRequester() }

                var searchText by rememberSaveable { mutableStateOf("") }
                LaunchedEffect(searchText) {
                    delay(300)
                    filterText = searchText
                }

                val isAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        if (isAtTop) Color.Unspecified
                        else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            if (hasChange) {
                                showUnsavedChangeDialog = true
                            } else {
                                navigator.pop()
                            }
                        }) {
                            Icon(painter = painterResource(R.drawable.ic_back), null)
                        }
                    },
                    title = {
                        if (!showSearchBar) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    positioning = TooltipAnchorPosition.Below
                                ),
                                tooltip = {
                                    PlainTooltip {
                                        val hapticFeedback = LocalHapticFeedback.current
                                        val clipboard = LocalClipboard.current
                                        LaunchedEffect(Unit) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            clipboard.setClipEntry(
                                                ClipEntry(
                                                    ClipData.newPlainText(
                                                        "name",
                                                        name
                                                    )
                                                )
                                            )
                                        }
                                        Text(name)
                                    }
                                },
                                state = rememberTooltipState()
                            ) {
                                Column {
                                    Text(
                                        text = "编辑",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )

                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.basicMarquee(
                                            repeatDelayMillis = 2_000,
                                        ),
                                    )
                                }
                            }
                        } else {
                            BackHandler {
                                searchText = ""
                                showSearchBar = false
                            }

                            val focusManager = LocalFocusManager.current
                            val keyboardController = LocalSoftwareKeyboardController.current
                            BasicTextField(
                                value = searchText,
                                onValueChange = {
                                    searchText = it
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 18.sp
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                },
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                                decorationBox = {
                                    TextFieldDefaults.DecorationBox(
                                        value = searchText,
                                        innerTextField = it,
                                        enabled = true,
                                        singleLine = true,
                                        visualTransformation = VisualTransformation.None,
                                        interactionSource = remember { MutableInteractionSource() },
                                        placeholder = {
                                            Text(
                                                modifier = Modifier.alpha(.78f),
                                                text = "搜索...",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            )
                                        },
                                        container = {}
                                    )
                                }
                            )

                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        }
                    },
                    actions = {
                        if (!showSearchBar) {
                            IconButton(onClick = { showSearchBar = true }) {
                                Icon(painter = painterResource(R.drawable.ic_search), null)
                            }
                        }
                        IconButton(
                            onClick = {
                                val path =
                                    "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}/$DOWNLOAD_SUBDIR_NAME"
                                if (uiState.texts.isNotEmpty()) {
                                    if (hasChange) {
                                        saveGameText(uiState.file, uiState.texts)
                                        hasChange = false
                                    }
                                    context.copyFileToDownloadFolder(uiState.file)
                                    screenModel.showDialog("资源已导出到 $path")
                                } else {
                                    screenModel.exportAsset(path)
                                }
                            }
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_export), null)
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            if (uiState.texts.isNotEmpty())
                                launcher.launch("*/*")
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            null
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FloatingActionButton(onClick = {
                        if (hasChange) {
                            saveGameText(uiState.file, uiState.texts)
                            hasChange = false
                        } else showPatchConfirmDialog = true
                    }) {
                        AnimatedContent(hasChange) { hasChange ->
                            Icon(
                                painter = painterResource(if (hasChange) R.drawable.ic_save else R.drawable.ic_patch),
                                null
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (uiState.loading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .drawVerticalScrollbar(listState),
                    state = listState
                ) {
                    items(
                        uiState.texts.filter {
                            it.id.contains(filterText) || it.text.contains(
                                filterText
                            )
                        },
                        key = { it.id }) { text ->
                        var value by remember { mutableStateOf(text.text) }
                        LaunchedEffect(uiState.texts) { value = text.text }
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .padding(top = 10.dp),
                            value = value,
                            onValueChange = {
                                text.text = it
                                value = it
                                hasChange = true
                            },
                            label = { Text(text.id) }
                        )
                    }
                }
            }
        }
    }
}