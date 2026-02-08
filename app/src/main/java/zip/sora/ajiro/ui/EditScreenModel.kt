package zip.sora.ajiro.ui

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zip.sora.ajiro.privileged.ExtractTextResult
import zip.sora.ajiro.privileged.UserService
import zip.sora.ajiro.privileged.toExtractTextResult
import zip.sora.ajiro.utils.GameText
import zip.sora.ajiro.utils.loadGameText
import java.io.File
import java.io.InputStream

data class EditScreenUiState(
    val loading: Boolean = true,
    val texts: List<GameText> = emptyList(),
    val file: File,
    val dialogMessage: String? = null
)

class EditScreenModel(
    private val name: String,
    private val fileDir: String
) : ScreenModel {
    private val _uiState =
        MutableStateFlow(
            EditScreenUiState(
                file = File(fileDir, name.dropLast(8))
            )
        )
    val uiState = _uiState.asStateFlow()

    fun resolveDialog() {
        _uiState.update { it.copy(dialogMessage = null) }
    }

    fun showDialog(text: String) {
        _uiState.update { it.copy(dialogMessage = text) }
    }

    fun import(inputStream: InputStream) {
        _uiState.update { it.copy(loading = true) }
        screenModelScope.launch(Dispatchers.Default) {
            val newTexts = try {
                loadGameText(inputStream)
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, dialogMessage = "导入文本时出错") }
                return@launch
            }
            uiState.value.texts.let { oldTexts ->
                if (newTexts.size != oldTexts.size) {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            dialogMessage = "导入的文本数量不匹配"
                        )
                    }
                    return@launch
                }
                oldTexts.forEachIndexed { index, oldText ->
                    if (newTexts[index].id != oldText.id) {
                        _uiState.update {
                            it.copy(
                                loading = false,
                                dialogMessage = "导入的文本ID不匹配"
                            )
                        }
                        return@launch
                    }
                }
            }
            _uiState.update { it.copy(loading = false, texts = newTexts) }
        }
    }

    fun exportAsset(path: String) {
        screenModelScope.launch(Dispatchers.IO) {
            UserService.instance?.let { service ->
                _uiState.update { it.copy(loading = true) }
                if (service.exportAsset(name, path)) {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            dialogMessage = "资源已导出到 $path"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            dialogMessage = ExtractTextResult.NOT_FOUND.message
                        )
                    }
                }
            }
        }
    }

    fun patch() {
        screenModelScope.launch(Dispatchers.IO) {
            UserService.instance?.let { service ->
                _uiState.update { it.copy(loading = true) }
                if (service.patchText(name, fileDir)) {
                    service.patchFinish()
                    _uiState.update { it.copy(loading = false, dialogMessage = "修补完成") }
                } else {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            dialogMessage = ExtractTextResult.NOT_FOUND.message
                        )
                    }
                }
            }
        }
    }

    init {
        File(fileDir).mkdirs()
        screenModelScope.launch(Dispatchers.Default) {
            UserService.instance?.extractText(name, fileDir)?.let { code ->
                val result = code.toExtractTextResult()
                if (result != ExtractTextResult.SUCCESS) {
                    _uiState.update { it.copy(dialogMessage = result.message, loading = false) }
                    return@launch
                }
            }
            loadGameText(_uiState.value.file).let { texts ->
                _uiState.update {
                    it.copy(texts = texts, loading = false)
                }
            }
        }
    }
}