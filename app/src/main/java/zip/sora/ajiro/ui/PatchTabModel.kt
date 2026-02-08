package zip.sora.ajiro.ui

import android.content.Context
import android.text.format.Formatter
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import zip.sora.ajiro.HARAPPA_DIR_NAME
import zip.sora.ajiro.HARAPPA_ZIP_NAME
import zip.sora.ajiro.privileged.UserService
import zip.sora.ajiro.safePatchSet
import zip.sora.ajiro.utils.downloadFile
import zip.sora.ajiro.utils.getLatestCommitSha
import zip.sora.ajiro.utils.getLocalSha
import zip.sora.ajiro.utils.setLocalSha
import zip.sora.ajiro.utils.unzipTo
import java.io.File

enum class PatchTabStatus {
    PENDING,
    DOWNLOADING,
    UNPACKING,
    PATCHING,
    RESTORING
}

data class PatchTabUiState(
    val localSha: String?,
    val remoteSha: String?,
    val errorMessage: String?,
    val status: PatchTabStatus,
    val progress: Float,
    val progressDesc: ((Context) -> String)?
)

class PatchTabModel : ScreenModel {
    private val _uiState = MutableStateFlow(
        PatchTabUiState(
            localSha = null,
            remoteSha = null,
            errorMessage = null,
            status = PatchTabStatus.PENDING,
            progress = 0.0f,
            progressDesc = null,
        )
    )

    val uiState = _uiState.asStateFlow()

    fun init(fileDir: String) {
        screenModelScope.launch {
            getLocalSha(fileDir)?.let { localSha ->
                _uiState.update { it.copy(localSha = localSha) }
            }
            getLatestCommitSha().let { remoteSha ->
                _uiState.update { it.copy(remoteSha = remoteSha ?: "") }
            }
        }
    }

    fun update(fileDir: String) {
        val url = "https://github.com/CrackTC/Harappa/archive/${uiState.value.remoteSha}.zip"
        val outputFile = File(fileDir, HARAPPA_ZIP_NAME)
        val outputDir = File(fileDir, HARAPPA_DIR_NAME)
        screenModelScope.launch {
            _uiState.update { it.copy(errorMessage = null, status = PatchTabStatus.DOWNLOADING) }
            try {
                downloadFile(url, outputFile) { downloaded, total ->
                    _uiState.update {
                        it.copy(
                            progress = downloaded.toFloat() / total,
                            progressDesc = { context ->
                                val a = Formatter.formatFileSize(context, downloaded)
                                val b = Formatter.formatFileSize(context, total)
                                "$a/$b"
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "下载失败: ${e.message}",
                        status = PatchTabStatus.PENDING
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(status = PatchTabStatus.UNPACKING) }
            try {
                outputFile.unzipTo(outputDir) { now, total, name ->
                    _uiState.update {
                        it.copy(
                            progress = now.toFloat() / total,
                            progressDesc = { name }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "解压失败: ${e.message}",
                        status = PatchTabStatus.PENDING
                    )
                }
                return@launch
            }
            setLocalSha(fileDir, uiState.value.remoteSha!!)
            _uiState.update {
                it.copy(
                    localSha = it.remoteSha,
                    status = PatchTabStatus.PENDING,
                    progressDesc = { "完成！" })
            }
        }
    }

    fun patch(fileDir: String) {
        _uiState.update { it.copy(status = PatchTabStatus.PATCHING) }

        screenModelScope.launch(Dispatchers.IO) {
            val harappa = File(fileDir, "$HARAPPA_DIR_NAME/unity3d")
            var total: Int
            harappa.listFiles { it.extension == "unity3d" }?.also { total = it.size }
                ?.forEachIndexed { index, file ->
                    _uiState.update {
                        it.copy(
                            progress = index.toFloat() / total,
                            progressDesc = { file.name }
                        )
                    }

                    if (!safePatchSet.contains(file.name)) {
                        UserService.instance?.quickPatchSingle(file.path)
                    } else {
                        UserService.instance?.patchText(
                            file.nameWithoutExtension,
                            File(fileDir, "$HARAPPA_DIR_NAME/gtx").path
                        )
                    }
                }
            UserService.instance?.patchFinish()
            _uiState.update { it.copy(status = PatchTabStatus.PENDING, progressDesc = { "完成！" }) }
        }
    }

    fun restore() {
        _uiState.update { it.copy(status = PatchTabStatus.RESTORING) }
        screenModelScope.launch(Dispatchers.IO) {
            UserService.instance?.restoreBackup()
            _uiState.update { it.copy(status = PatchTabStatus.PENDING, progressDesc = { "完成！" }) }
        }
    }
}